package plugins

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.rudderstack.android.storage.AndroidStorage
import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.models.RudderOption
import com.rudderstack.core.internals.plugins.Plugin
import com.rudderstack.core.internals.storage.StorageKeys
import com.rudderstack.core.internals.utils.empty
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.atomic.AtomicBoolean
import com.rudderstack.android.Configuration as AndroidConfiguration

// plugin to manage default lifecycle events
internal class AndroidLifecyclePlugin : Plugin, DefaultLifecycleObserver {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Manual
    override lateinit var analytics: Analytics

    private lateinit var lifecycle: Lifecycle
    private lateinit var storage: AndroidStorage
    private lateinit var packageInfo: PackageInfo
    private lateinit var application: Application

    // state variables
    private var shouldTrackApplicationLifecycleEvents: Boolean = true
    private val firstLaunch = AtomicBoolean(false)
    private val trackedApplicationLifecycleEvents = AtomicBoolean(false)

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        (analytics.configuration as? AndroidConfiguration)?.let { config ->
            application = config.application as? Application
                ?: logAndThrowError("no android application context registered")
            shouldTrackApplicationLifecycleEvents = config.trackApplicationLifecycleEvents
            storage = config.storageProvider as? AndroidStorage ?: logAndThrowError("Android Storage required")
        }
        lifecycle = ProcessLifecycleOwner.get().lifecycle

        val packageManager: PackageManager = application.packageManager
        packageInfo = try {
            packageManager.getPackageInfo(application.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            val message = "Package not found: ${application.packageName}"
            logAndThrowError(message = message, throwable = e)
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

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        if (shouldTrackApplicationLifecycleEvents && !trackedApplicationLifecycleEvents.getAndSet(true)) {
            firstLaunch.set(true)
            trackApplicationLifecycleEvents()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (shouldTrackApplicationLifecycleEvents) {
            val properties = buildJsonObject {
                if (firstLaunch.get()) {
                    put(VERSION_KEY, packageInfo.versionName)
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
        // Get the current version.
        val packageInfo = packageInfo
        val currentVersion = packageInfo.versionName
        val currentBuild = packageInfo.getVersionCode()

        // Get the previous recorded version.
        val previousVersion = storage.readString(StorageKeys.APP_VERSION, String.empty())
        val previousBuild = storage.readLong(StorageKeys.APP_BUILD, -1L)

        // Check and track Application Installed or Application Updated.
        if (previousBuild == -1L) {
            analytics.track(
                APPLICATION_INSTALLED,
                buildJsonObject {
                    put(VERSION_KEY, currentVersion)
                    put(BUILD_KEY, currentBuild)
                },
                RudderOption()
            )
        } else if (currentBuild != previousBuild) {
            analytics.track(
                APPLICATION_UPDATED,
                buildJsonObject {
                    put(VERSION_KEY, currentVersion)
                    put(BUILD_KEY, currentBuild)
                    put("previous_$VERSION_KEY", previousVersion)
                    put("previous_$BUILD_KEY", previousBuild)
                },
                RudderOption()
            )
        }

        // Update the recorded version.
        runOnAnalyticsThread {
            storage.write(StorageKeys.APP_VERSION, currentVersion)
            storage.write(StorageKeys.APP_BUILD, currentBuild.toLong())
        }
    }

    private fun runOnMainThread(block: () -> Unit) = with(analytics) {
        analyticsScope.launch(mainDispatcher) {
            block()
        }
    }

    private fun runOnAnalyticsThread(block: suspend () -> Unit) = with(analytics) {
        analyticsScope.launch(analyticsDispatcher) {
            block()
        }
    }

    private fun logAndThrowError(message: String, throwable: Throwable? = null): Nothing {
        analytics.configuration.logger.error(log = message)
        throw throwable ?: error(message)
    }

    companion object {

        private const val APPLICATION_INSTALLED = "Application Installed"
        private const val APPLICATION_OPENED = "Application Opened"
        private const val APPLICATION_UPDATED = "Application Updated"
        private const val APPLICATION_BACKGROUNDED = "Application Backgrounded"
        private const val VERSION_KEY = "version"
        private const val BUILD_KEY = "build"
        private const val FROM_BACKGROUND = "from_background"
    }
}

private fun PackageInfo.getVersionCode(): Number = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    this.longVersionCode
} else {
    @Suppress("DEPRECATION")
    this.versionCode
}
