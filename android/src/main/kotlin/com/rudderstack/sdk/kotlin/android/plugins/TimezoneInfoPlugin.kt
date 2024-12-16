package com.rudderstack.sdk.kotlin.android.plugins

import com.rudderstack.sdk.kotlin.android.utils.mergeWithHigherPriorityTo
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.annotations.VisibleForTesting
import java.util.TimeZone

private const val TIMEZONE_KEY = "timezone"

/**
 * Plugin to attach timezone info to the event context payload
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

    override suspend fun intercept(event: Event): Event = attachTimezoneInfo(event)

    private fun attachTimezoneInfo(event: Event): Event {
        LoggerAnalytics.debug("Attaching timezone info to the event payload")

        event.context = event.context mergeWithHigherPriorityTo timezoneContext

        return event
    }
}
