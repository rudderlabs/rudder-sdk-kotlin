package com.rudderstack.android.sdk.plugins

import com.rudderstack.android.sdk.utils.mergeWithHigherPriorityTo
import com.rudderstack.kotlin.core.Analytics
import com.rudderstack.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.kotlin.core.internals.models.Message
import com.rudderstack.kotlin.core.internals.plugins.Plugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.annotations.VisibleForTesting
import java.util.TimeZone

private const val TIMEZONE_KEY = "timezone"

/**
 * Plugin to attach timezone info to the message context payload
 */
internal class TimezoneInfoPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    private lateinit var timezoneContext: JsonObject

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        timezoneContext = constructTimezoneContext()
    }

    @VisibleForTesting
    internal fun constructTimezoneContext(): JsonObject = buildJsonObject {
        put(TIMEZONE_KEY, TimeZone.getDefault().id)
    }

    override suspend fun execute(message: Message): Message = attachTimezoneInfo(message)

    private fun attachTimezoneInfo(message: Message): Message {
        LoggerAnalytics.debug("Attaching timezone info to the message payload")

        message.context = message.context mergeWithHigherPriorityTo timezoneContext

        return message
    }
}
