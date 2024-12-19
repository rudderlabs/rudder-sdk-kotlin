package com.rudderstack.sdk.kotlin.android.plugins

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.utils.mergeWithHigherPriorityTo
import com.rudderstack.sdk.kotlin.android.utils.putIfNotNull
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.annotations.VisibleForTesting

private const val APP_KEY = "app"
private const val APP_BUILD_KEY = "build"
private const val APP_NAME_KEY = "name"
private const val APP_NAMESPACE_KEY = "namespace"
private const val APP_VERSION_KEY = "version"

/**
 * Plugin to attach app info to the event context payload
 *
 * **NOTE**: This plugin needs to be added early in the plugin chain to ensure that the app info is attached to all events
 */
internal class AppInfoPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    private lateinit var appContext: JsonObject

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        (analytics.configuration as Configuration).let { config ->
            appContext = try {
                val packageManager = config.application.packageManager
                val packageInfo = packageManager.getPackageInfo(config.application.packageName, 0)
                constructAppContext(packageInfo, packageManager)
            } catch (e: PackageManager.NameNotFoundException) {
                LoggerAnalytics.error("Failed to get package info", e)
                emptyJsonObject
            }
        }
    }

    @VisibleForTesting
    internal fun constructAppContext(packageInfo: PackageInfo, packageManager: PackageManager): JsonObject =
        packageInfo.let {
            val appBuild = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                it.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                it.versionCode.toString()
            }

            buildJsonObject {
                put(
                    APP_KEY,
                    buildJsonObject {
                        putIfNotNull(APP_NAME_KEY, it.applicationInfo?.loadLabel(packageManager))
                        put(APP_NAMESPACE_KEY, it.packageName)
                        put(APP_VERSION_KEY, it.versionName)
                        put(APP_BUILD_KEY, appBuild)
                    }
                )
            }
        }

    override suspend fun intercept(event: Event): Event = attachAppInfo(event)

    private fun attachAppInfo(event: Event): Event {
        LoggerAnalytics.debug("Attaching app info to the event payload")

        event.context = event.context mergeWithHigherPriorityTo appContext

        return event
    }
}
