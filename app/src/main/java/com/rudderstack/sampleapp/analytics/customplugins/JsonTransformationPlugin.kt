package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.dmt.QuickJS
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class JsonTransformationPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.OnProcess
    override lateinit var analytics: Analytics

    private val quickJS = QuickJS()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun intercept(event: Event): Event? {
        return when (event) {
            is TrackEvent -> {
                try {
                    val originalJson = json.encodeToString(event)
                    LoggerAnalytics.debug("JsonTransformationPlugin: Original JSON - $originalJson")
                    
                    val transformedJson = quickJS.transformJson(originalJson)
                    LoggerAnalytics.debug("JsonTransformationPlugin: Transformed JSON - $transformedJson")
                    
                    val transformedEvent = json.decodeFromString<TrackEvent>(transformedJson)
                    LoggerAnalytics.debug("JsonTransformationPlugin: Successfully transformed track event '${event.event}' to '${transformedEvent.event}'")
                    
                    transformedEvent
                } catch (e: Exception) {
                    LoggerAnalytics.error("JsonTransformationPlugin: Error transforming event - ${e.message}")
                    event
                }
            }
            else -> {
                LoggerAnalytics.debug("JsonTransformationPlugin: Skipping non-track event of type ${event.type}")
                event
            }
        }
    }
}
