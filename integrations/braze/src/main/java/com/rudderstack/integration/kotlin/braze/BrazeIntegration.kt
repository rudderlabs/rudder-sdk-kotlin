package com.rudderstack.integration.kotlin.braze

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin

/**
 * BrazeIntegration is a plugin that sends events to the Braze SDK.
 */
class BrazeIntegration : Plugin {
    override val pluginType = Plugin.PluginType.OnProcess

    override lateinit var analytics: Analytics

    override suspend fun intercept(event: Event): Event {
        if (event is TrackEvent) {
            println("BrazeIntegration: Track event is made ${event.event}")
        }
        return event
    }
}
