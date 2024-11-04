package com.rudderstack.android.sdk.plugins

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.rudderstack.android.sdk.Configuration
import com.rudderstack.android.sdk.utils.mergeWithHigherPriorityTo
import com.rudderstack.android.sdk.utils.putUndefinedIfNull
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.LoggerAnalytics
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.models.emptyJsonObject
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import org.jetbrains.annotations.VisibleForTesting

private const val APP_KEY = "app"
private const val APP_BUILD_KEY = "build"
private const val APP_NAME_KEY = "name"
private const val APP_NAMESPACE_KEY = "namespace"
private const val APP_VERSION_KEY = "version"

/**
 * Plugin to attach app info to the message context payload
 *
 * **NOTE**: This plugin needs to be added early in the plugin chain to ensure that the app info is attached to all messages
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
                        putUndefinedIfNull(APP_NAME_KEY, it.applicationInfo.loadLabel(packageManager))
                        putUndefinedIfNull(APP_NAMESPACE_KEY, it.packageName)
                        putUndefinedIfNull(APP_VERSION_KEY, it.versionName)
                        putUndefinedIfNull(APP_BUILD_KEY, appBuild)
                    }
                )
            }
        }

    override fun execute(message: Message): Message = attachAppInfo(message)

    private fun attachAppInfo(message: Message): Message {
        LoggerAnalytics.debug("Attaching app info to the message payload")

        message.context = message.context mergeWithHigherPriorityTo appContext

        return message
    }
}
