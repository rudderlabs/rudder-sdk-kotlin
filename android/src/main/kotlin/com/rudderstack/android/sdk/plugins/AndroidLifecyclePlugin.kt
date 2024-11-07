package com.rudderstack.android.sdk.plugins

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import com.rudderstack.android.sdk.models.AppVersion
import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ProcessLifecycleObserver
import com.rudderstack.android.sdk.utils.addLifecycleObserver
import com.rudderstack.android.sdk.utils.logAndThrowError
import com.rudderstack.android.sdk.utils.runOnAnalyticsThread
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.RudderOption
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import com.rudderstack.kotlin.sdk.internals.storage.Storage
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys
import com.rudderstack.kotlin.sdk.internals.utils.empty
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.Delegates
import com.rudderstack.android.sdk.Analytics as AndroidAnalytics
import com.rudderstack.android.sdk.Configuration as AndroidConfiguration

internal const val APPLICATION_INSTALLED = "Application Installed"
internal const val APPLICATION_OPENED = "Application Opened"
internal const val APPLICATION_UPDATED = "Application Updated"
internal const val APPLICATION_BACKGROUNDED = "Application Backgrounded"
internal const val VERSION_KEY = "version"
internal const val BUILD_KEY = "build"
internal const val FROM_BACKGROUND = "from_background"

// plugin to manage default lifecycle events
internal class AndroidLifecyclePlugin : Plugin, ProcessLifecycleObserver {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Manual
    override lateinit var analytics: Analytics

    private lateinit var storage: Storage
    private lateinit var appVersion: AppVersion
    private lateinit var application: Application

    // state variables
    private var shouldTrackApplicationLifecycleEvents by Delegates.notNull<Boolean>()
    private val firstLaunch = AtomicBoolean(true)

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        (analytics.configuration as? AndroidConfiguration)?.let { config ->
            application = config.application
            shouldTrackApplicationLifecycleEvents = config.trackApplicationLifecycleEvents
            storage = config.storage
        }

        // update the app version code and build regardless of tracking enabled or not.
        appVersion = getAppVersion()
        updateAppVersion()

        if (shouldTrackApplicationLifecycleEvents) {
            trackApplicationLifecycleEvents()
            (analytics as? AndroidAnalytics)?.addLifecycleObserver(this)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        val properties = buildJsonObject {
            if (firstLaunch.get()) {
                put(VERSION_KEY, appVersion.currentVersionName)
            }
            put(FROM_BACKGROUND, !firstLaunch.getAndSet(false))
        }
        analytics.track(APPLICATION_OPENED, properties, RudderOption())
    }

    override fun onStop(owner: LifecycleOwner) {
        analytics.track(APPLICATION_BACKGROUNDED, options = RudderOption())
    }

    private fun trackApplicationLifecycleEvents() {
        // Check and track Application Installed or Application Updated.
        if (appVersion.previousBuild == -1L) {
            analytics.track(
                APPLICATION_INSTALLED,
                buildJsonObject {
                    put(VERSION_KEY, appVersion.currentVersionName)
                    put(BUILD_KEY, appVersion.currentBuild)
                },
                RudderOption()
            )
        } else if (appVersion.currentBuild != appVersion.previousBuild) {
            analytics.track(
                APPLICATION_UPDATED,
                buildJsonObject {
                    put(VERSION_KEY, appVersion.currentVersionName)
                    put(BUILD_KEY, appVersion.currentBuild)
                    put("previous_$VERSION_KEY", appVersion.previousVersionName)
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
            analytics.logAndThrowError(message = message, throwable = e)
        }

        return AppVersion(
            currentVersionName = packageInfo.versionName,
            currentBuild = packageInfo.getVersionCode().toLong(),
            previousVersionName = storage.readString(StorageKeys.APP_VERSION, String.empty()),
            previousBuild = storage.readLong(StorageKeys.APP_BUILD, -1L)
        )
    }

    private fun updateAppVersion() {
        analytics.runOnAnalyticsThread {
            storage.write(StorageKeys.APP_VERSION, appVersion.currentVersionName)
            storage.write(StorageKeys.APP_BUILD, appVersion.currentBuild)
        }
    }
}

private fun PackageInfo.getVersionCode(): Number = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    this.longVersionCode
} else {
    @Suppress("DEPRECATION")
    this.versionCode
}
