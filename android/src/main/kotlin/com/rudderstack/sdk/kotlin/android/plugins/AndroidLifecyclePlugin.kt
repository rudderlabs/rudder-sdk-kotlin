package com.rudderstack.sdk.kotlin.android.plugins

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import com.rudderstack.sdk.kotlin.android.models.AppVersion
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ProcessLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.addLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.logAndThrowError
import com.rudderstack.sdk.kotlin.android.utils.putIfNotNull
import com.rudderstack.sdk.kotlin.android.utils.removeLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.runOnAnalyticsThread
import com.rudderstack.sdk.kotlin.android.utils.runOnAnalyticsThreadAfter
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import kotlinx.coroutines.Job
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.atomic.AtomicBoolean
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics
import com.rudderstack.sdk.kotlin.android.Configuration as AndroidConfiguration

internal const val APPLICATION_INSTALLED = "Application Installed"
internal const val APPLICATION_OPENED = "Application Opened"
internal const val APPLICATION_UPDATED = "Application Updated"
internal const val APPLICATION_BACKGROUNDED = "Application Backgrounded"
internal const val VERSION_KEY = "version"
internal const val BUILD_KEY = "build"
internal const val FROM_BACKGROUND = "from_background"

// plugin to manage default lifecycle events
internal class AndroidLifecyclePlugin : Plugin, ProcessLifecycleObserver {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Utility
    override lateinit var analytics: Analytics

    private lateinit var storage: Storage
    private lateinit var appVersion: AppVersion
    private lateinit var application: Application

    // state variables
    private val firstLaunch = AtomicBoolean(true)
    private var lastLifecycleJob: Job? = null

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        (analytics.configuration as? AndroidConfiguration)?.let { config ->
            application = config.application
            storage = analytics.storage
            appVersion = getAppVersion()
            lastLifecycleJob = analytics.runOnAnalyticsThread {
                // Persist the app version before the install/update event is queued, so a process that
                // dies right after the event is queued does not re-fire it on the next launch.
                updateAppVersion()
                if (config.trackApplicationLifecycleEvents) {
                    trackApplicationLifecycleEvents()
                }
            }
            if (config.trackApplicationLifecycleEvents) {
                (analytics as? AndroidAnalytics)?.addLifecycleObserver(this)
            }
        }
    }

    override fun teardown() {
        (analytics as? AndroidAnalytics)?.removeLifecycleObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        val isFirstLaunch = firstLaunch.getAndSet(false)
        lastLifecycleJob = analytics.runOnAnalyticsThreadAfter(lastLifecycleJob) {
            trackApplicationOpened(isFirstLaunch)
        }
    }

    private fun trackApplicationOpened(isFirstLaunch: Boolean) {
        val properties = buildJsonObject {
            if (isFirstLaunch) {
                putIfNotNull(VERSION_KEY, appVersion.currentVersionName)
            }
            put(FROM_BACKGROUND, !isFirstLaunch)
        }
        analytics.track(APPLICATION_OPENED, properties, RudderOption())
    }

    override fun onStop(owner: LifecycleOwner) {
        lastLifecycleJob = analytics.runOnAnalyticsThreadAfter(lastLifecycleJob) {
            analytics.track(APPLICATION_BACKGROUNDED, options = RudderOption())
        }
    }

    private fun trackApplicationLifecycleEvents() {
        // Check and track Application Installed or Application Updated.
        if (appVersion.previousBuild == -1L) {
            analytics.track(
                APPLICATION_INSTALLED,
                buildJsonObject {
                    putIfNotNull(VERSION_KEY, appVersion.currentVersionName)
                    put(BUILD_KEY, appVersion.currentBuild)
                },
                RudderOption()
            )
        } else if (appVersion.currentBuild != appVersion.previousBuild) {
            analytics.track(
                APPLICATION_UPDATED,
                buildJsonObject {
                    putIfNotNull(VERSION_KEY, appVersion.currentVersionName)
                    put(BUILD_KEY, appVersion.currentBuild)
                    putIfNotNull("previous_$VERSION_KEY", appVersion.previousVersionName)
                    put("previous_$BUILD_KEY", appVersion.previousBuild)
                },
                RudderOption()
            )
        }
    }

    @VisibleForTesting
    internal fun getAppVersion(): AppVersion {
        val packageManager: PackageManager = application.packageManager
        val packageInfo = try {
            packageManager.getPackageInfo(application.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            val message = "Package not found: ${application.packageName}"
            logAndThrowError(message = message, throwable = e, logger = analytics.logger)
        }

        return AppVersion(
            currentVersionName = packageInfo.versionName,
            currentBuild = packageInfo.getVersionCode().toLong(),
            previousVersionName = storage.readString(StorageKeys.APP_VERSION, String.empty()).ifEmpty { null },
            previousBuild = storage.readLong(StorageKeys.APP_BUILD, -1L)
        )
    }

    private suspend fun updateAppVersion() {
        if (appVersion.currentBuild != appVersion.previousBuild) {
            storage.write(StorageKeys.APP_BUILD, appVersion.currentBuild)
        }
        appVersion.currentVersionName?.let { currentVersionName ->
            if (currentVersionName != appVersion.previousVersionName) {
                storage.write(StorageKeys.APP_VERSION, currentVersionName)
            }
        }
    }
}

private fun PackageInfo.getVersionCode(): Number = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    this.longVersionCode
} else {
    @Suppress("DEPRECATION")
    this.versionCode
}
