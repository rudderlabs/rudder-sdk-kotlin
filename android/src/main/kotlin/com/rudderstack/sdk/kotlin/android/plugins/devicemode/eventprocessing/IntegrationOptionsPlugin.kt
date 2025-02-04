package com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing

import com.rudderstack.sdk.kotlin.android.utils.getBoolean
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin

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
