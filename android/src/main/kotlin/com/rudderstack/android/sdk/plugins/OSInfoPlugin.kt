package com.rudderstack.android.sdk.plugins

import android.os.Build
import com.rudderstack.android.sdk.utils.mergeWithHigherPriorityTo
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
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

    override fun execute(message: Message): Message = attachOSInfo(message)

    private fun attachOSInfo(message: Message): Message {
        analytics.configuration.logger.debug(log = "Attaching OS info to the message payload")

        message.context = message.context mergeWithHigherPriorityTo osContext

        return message
    }
}
