package com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing

import com.rudderstack.sdk.kotlin.android.utils.getBoolean
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin

/**
 * A plugin to pass or drop events based on the integration options set for a destination in events.
 *
 * The plugin checks if the destination is explicitly enabled or disabled AND
 * if all destinations are enabled or disabled in the event's integrations
 * and then passes or drops the event accordingly.
 *
 * It applies the below logic to decide whether to pass or drop the events:
 *
 * All -> true - Allow
 * All -> false - Block
 *
 * All -> false && key = true - Allow
 * All -> false && key = false - Block
 *
 * key = true - Allow
 * key = false - Block
 *
 * **Note**: Since integrations can be a `JsonObject`, this plugin also handle
 * scenarios where integrations is set to some complex object.
 */
internal class IntegrationOptionsPlugin(
    private val key: String
) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    override suspend fun intercept(event: Event): Event? {
        val integrationOptions = event.integrations

        integrationOptions.getBoolean(key)?.let { isExplicitlyEnabled ->
            return when (isExplicitlyEnabled) {
                true -> event
                false -> {
                    logDroppedEvent(event)
                    null
                }
            }
        }

        integrationOptions.getBoolean("All")?.let { isAllEnabled ->
            return when (isAllEnabled) {
                true -> event
                false -> {
                    logDroppedEvent(event)
                    null
                }
            }
        }

        return event
    }

    private fun logDroppedEvent(event: Event) {
        LoggerAnalytics.debug("IntegrationOptionsPlugin: Dropped event $event for destination: $key")
    }
}
