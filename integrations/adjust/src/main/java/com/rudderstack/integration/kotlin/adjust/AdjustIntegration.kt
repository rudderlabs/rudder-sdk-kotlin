package com.rudderstack.integration.kotlin.adjust

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin

/**
 * AdjustIntegration is a plugin that intercepts the track events and logs them.
 */
class AdjustIntegration : Plugin {
    override val pluginType = Plugin.PluginType.OnProcess

    override lateinit var analytics: Analytics

    override suspend fun intercept(event: Event): Event {
        if (event is TrackEvent) {
            println("AdjustIntegration: Track event is made ${event.event}")
        }
        return event
    }
}
