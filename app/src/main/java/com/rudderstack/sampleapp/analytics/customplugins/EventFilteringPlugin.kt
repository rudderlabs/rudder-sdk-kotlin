package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin

/**
 * This plugin filters out specific analytics events from being processed in the analytics pipeline. 
 * It allows you to prevent certain events from being tracked or sent to destinations.
 * 
 * ## Usage:
 * ```kotlin
 * // Create and add the plugin with default events
 * val eventFilteringPlugin = EventFilteringPlugin()
 * analytics.add(eventFilteringPlugin)
 * 
 * // Create and add the plugin with custom events
 * val eventFilteringPlugin = EventFilteringPlugin(listOf("Custom Event", "Another Event"))
 * analytics.add(eventFilteringPlugin)
 * ```
 */
class EventFilteringPlugin(
    private val eventsToFilter: List<String> = listOf("Application Opened", "Application Backgrounded")
) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.OnProcess
    override lateinit var analytics: Analytics

    override suspend fun intercept(event: Event): Event? {
        if (event is TrackEvent && eventsToFilter.contains(event.event)) {
            LoggerAnalytics.verbose("EventFilteringPlugin: Event \"${event.event}\" is filtered out.")
            return null
        }
        return event
    }
}
