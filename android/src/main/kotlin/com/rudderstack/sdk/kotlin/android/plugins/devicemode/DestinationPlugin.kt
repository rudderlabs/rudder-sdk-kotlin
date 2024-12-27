package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.utils.getBoolean
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

abstract class DestinationPlugin : Plugin {

    final override val pluginType: Plugin.PluginType = Plugin.PluginType.Destination

    final override lateinit var analytics: Analytics

    abstract val key: String

    var isDestinationDisabled: Boolean = false
        private set

    open fun create(destinationConfig: JsonObject, analytics: Analytics, config: Configuration): Any? {
        return null
    }

    final override fun setup(analytics: Analytics) {
        super.setup(analytics)

        analytics.analyticsScope.launch(analytics.analyticsDispatcher) {
            val sourceConfig = analytics.sourceConfigState.first()
            val configDestination = findDestination(sourceConfig)
            isDestinationDisabled = findDestination(sourceConfig)?.isDestinationEnabled.isFalseOrNull()

            if (isDestinationDisabled) {
                LoggerAnalytics.warn("DestinationPlugin: Destination $key is disabled")
                return@launch
            }
            configDestination?.let {
                val destination = create(it.destinationConfig, analytics, analytics.configuration as Configuration)
                onIntegrationReady(destination)
            }
        }
    }

    final override suspend fun intercept(event: Event): Event? {
        return process(event)
    }

    private fun process(event: Event): Event? {
        if (isDestinationDisabledInOption(event) || isDestinationDisabled) {
            return event
        }

        when (val eventCopy = event.copy<Event>()) {
            is TrackEvent -> track(eventCopy)
            is ScreenEvent -> screen(eventCopy)
            is GroupEvent -> group(eventCopy)
            is IdentifyEvent -> identify(eventCopy)
            is AliasEvent -> alias(eventCopy)
        }
        return event
    }

    open fun onIntegrationReady(destination: Any?) {}

    open fun track(event: TrackEvent) {}

    open fun screen(event: ScreenEvent) {}

    open fun group(event: GroupEvent) {}

    open fun identify(event: IdentifyEvent) {}

    open fun alias(event: AliasEvent) {}

    open fun flush() {}

    open fun reset() {}

    private fun isDestinationDisabledInOption(event: Event): Boolean {
        val integrationOptions = event.integrations
        return integrationOptions.getBoolean(key) == false || (
            integrationOptions.getBoolean("All")
                .isFalseOrNull() && integrationOptions.getBoolean(key).isFalseOrNull()
            )
    }

    private fun findDestination(sourceConfig: SourceConfig): Destination? {
        return sourceConfig.source.destinations.firstOrNull { it.destinationDefinition.displayName == key }
    }

    private fun Boolean?.isFalseOrNull() = this == false || this == null
}
