package com.rudderstack.sdk.kotlin.android.plugins

import android.os.Build
import com.rudderstack.sdk.kotlin.android.utils.mergeWithHigherPriorityTo
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Message
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.annotations.VisibleForTesting

private const val OS_KEY = "os"
private const val OS_NAME_KEY = "name"
private const val OS_VERSION_KEY = "version"

private const val OS_VALUE = "Android"

/**
 * Plugin to attach OS info to the message context payload
 */
internal class OSInfoPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    private lateinit var osContext: JsonObject

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        osContext = constructAppContext()
    }

    @VisibleForTesting
    internal fun constructAppContext(): JsonObject = buildJsonObject {
        put(
            OS_KEY,
            buildJsonObject {
                put(OS_NAME_KEY, OS_VALUE)
                put(OS_VERSION_KEY, Build.VERSION.RELEASE)
            }
        )
    }

    override suspend fun execute(message: Message): Message = attachOSInfo(message)

    private fun attachOSInfo(message: Message): Message {
        LoggerAnalytics.debug("Attaching OS info to the message payload")

        message.context = message.context mergeWithHigherPriorityTo osContext

        return message
    }
}
