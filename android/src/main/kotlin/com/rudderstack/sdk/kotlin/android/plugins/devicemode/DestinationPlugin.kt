package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing.EventProcessorFacade
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing.IntegrationOptionsProcessor
import com.rudderstack.sdk.kotlin.android.utils.isFalseOrNull
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Destination
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.serialization.json.JsonObject

abstract class DestinationPlugin : Plugin {

    final override val pluginType: Plugin.PluginType = Plugin.PluginType.Destination

    final override lateinit var analytics: Analytics

    abstract val key: String

    var isDestinationDisabledInSource: Boolean = false
        private set

    private val eventProcessorFacade = EventProcessorFacade(listOf(IntegrationOptionsProcessor()))

    open fun create(destinationConfig: JsonObject, analytics: Analytics, config: Configuration): Any? {
        return null
    }

    final override fun setup(analytics: Analytics) {
        super.setup(analytics)
    }

    internal fun initialize(sourceConfig: SourceConfig) {
        val configDestination = findDestination(sourceConfig)
        isDestinationDisabledInSource = configDestination?.isDestinationEnabled.isFalseOrNull()

        if (isDestinationDisabledInSource) {
            LoggerAnalytics.warn("DestinationPlugin: Destination $key is disabled")
            return
        }
        configDestination?.let {
            val destination = create(it.destinationConfig, analytics, analytics.configuration as Configuration)
            onDestinationReady(destination)
        }
    }

    final override suspend fun intercept(event: Event): Event? {
        return process(event)
    }

    private fun process(event: Event): Event? {
        // eventProcessorFacade applies all the destination specific modifications and filtering to an event
        val processedEvent = findDestination(analytics.sourceConfigState.value)?.let {
            eventProcessorFacade.process(event, key, it)
        }

        if (processedEvent == null || isDestinationDisabledInSource) {
            return event
        }

        when (processedEvent) {
            is TrackEvent -> track(processedEvent)
            is ScreenEvent -> screen(processedEvent)
            is GroupEvent -> group(processedEvent)
            is IdentifyEvent -> identify(processedEvent)
            is AliasEvent -> alias(processedEvent)
        }
        return event
    }

    open fun onDestinationReady(destination: Any?) {}

    open fun track(event: TrackEvent) {}

    open fun screen(event: ScreenEvent) {}

    open fun group(event: GroupEvent) {}

    open fun identify(event: IdentifyEvent) {}

    open fun alias(event: AliasEvent) {}

    open fun flush() {}

    open fun reset() {}

    private fun findDestination(sourceConfig: SourceConfig): Destination? {
        return sourceConfig.source.destinations.firstOrNull { it.destinationDefinition.displayName == key }
    }
}
