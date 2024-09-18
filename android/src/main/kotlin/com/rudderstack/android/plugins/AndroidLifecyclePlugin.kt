package com.rudderstack.android.plugins

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.rudderstack.android.models.AppVersion
import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.models.RudderOption
import com.rudderstack.core.internals.plugins.Plugin
import com.rudderstack.core.internals.storage.Storage
import com.rudderstack.core.internals.storage.StorageKeys
import com.rudderstack.core.internals.utils.empty
import com.rudderstack.core.internals.utils.logAndThrowError
import com.rudderstack.core.internals.utils.runOnAnalyticsThread
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.atomic.AtomicBoolean
import com.rudderstack.android.Configuration as AndroidConfiguration

@DelicateCoroutinesApi
private val MAIN_DISPATCHER = Dispatchers.Main
internal const val APPLICATION_INSTALLED = "Application Installed"
internal const val APPLICATION_OPENED = "Application Opened"
internal const val APPLICATION_UPDATED = "Application Updated"
internal const val APPLICATION_BACKGROUNDED = "Application Backgrounded"
internal const val VERSION_KEY = "version"
internal const val BUILD_KEY = "build"
internal const val FROM_BACKGROUND = "from_background"

// plugin to manage default lifecycle events
internal class AndroidLifecyclePlugin : Plugin, DefaultLifecycleObserver {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Manual
    override lateinit var analytics: Analytics

    private lateinit var lifecycle: Lifecycle
    private lateinit var storage: Storage
    private lateinit var appVersion: AppVersion
    private lateinit var application: Application

    // state variables
    private var shouldTrackApplicationLifecycleEvents: Boolean = true
    private val firstLaunch = AtomicBoolean(true)

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        (analytics.configuration as? AndroidConfiguration)?.let { config ->
            application = config.application
            shouldTrackApplicationLifecycleEvents = config.trackApplicationLifecycleEvents
            storage = config.storage
        }
        lifecycle = getProcessLifecycle()

        // update the app version code and build regardless of tracking enabled or not.
        appVersion = getAppVersion()
        updateAppVersion()

        if (shouldTrackApplicationLifecycleEvents) {
            trackApplicationLifecycleEvents()
        }

        runOnMainThread {
            lifecycle.addObserver(this)
        }
    }

    override fun teardown() {
        super.teardown()
        if (shouldTrackApplicationLifecycleEvents) {
            runOnMainThread {
                lifecycle.removeObserver(this)
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (shouldTrackApplicationLifecycleEvents) {
            val properties = buildJsonObject {
                if (firstLaunch.get()) {
                    put(VERSION_KEY, appVersion.currentVersionName)
                }
                put(FROM_BACKGROUND, !firstLaunch.getAndSet(false))
            }
            analytics.track(APPLICATION_OPENED, properties, RudderOption())
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (shouldTrackApplicationLifecycleEvents) {
            analytics.track(APPLICATION_BACKGROUNDED, options = RudderOption())
        }
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
    internal fun getProcessLifecycle(): Lifecycle {
        return ProcessLifecycleOwner.get().lifecycle
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun runOnMainThread(block: () -> Unit) = with(analytics) {
        analyticsScope.launch(MAIN_DISPATCHER) {
            block()
        }
    }
}

private fun PackageInfo.getVersionCode(): Number = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    this.longVersionCode
} else {
    @Suppress("DEPRECATION")
    this.versionCode
}
