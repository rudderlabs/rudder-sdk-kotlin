package com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing

import com.rudderstack.sdk.kotlin.android.utils.findDestination
import com.rudderstack.sdk.kotlin.android.utils.getString
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

private const val WHITE_LIST_EVENTS = "whitelistedEvents"
private const val BLACK_LIST_EVENTS = "blacklistedEvents"
private const val EVENT_FILTERING_OPTION = "eventFilteringOption"

/**
 * A plugin to filter events based on the event filtering option provided in the destination config.
 *
 * This plugin filters the events based on the event filtering option provided in the destination config.
 * The plugin supports two types of event filtering options based on the dashboard configuration:
 * 1. Whitelist events: Only the events present in the whitelist will be allowed.
 * 2. Blacklist events: All the events except the ones present in the blacklist will be allowed.
 */
internal class EventFilteringPlugin(private val key: String) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    @Volatile
    private var filteringRule = FilteringRule()

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        listenForConfigChanges()
    }

    override suspend fun intercept(event: Event): Event? {
        if (event !is TrackEvent) {
            return event
        }

        val eventName = event.event.trim()
        return when {
            shouldDropEvent(eventName) -> {
                analytics.logger.debug(
                    "EventFilteringPlugin: Dropped event '$eventName' for destination: $key (messageId=${event.messageId})"
                )
                null
            }
            else -> event
        }
    }

    override fun teardown() {
        filteringRule = FilteringRule()
    }

    private fun shouldDropEvent(eventName: String): Boolean {
        val rule = filteringRule
        return when (rule.option) {
            WHITE_LIST_EVENTS -> eventName !in rule.events
            BLACK_LIST_EVENTS -> eventName in rule.events
            else -> false
        }
    }

    private fun listenForConfigChanges() {
        analytics.analyticsScope.launch(analytics.integrationsDispatcher) {
            analytics.sourceConfigState
                .observeDispatched()
                .filter { it.source.isSourceEnabled }
                .collect { sourceConfig ->
                    val destinationConfig = findDestination(sourceConfig, key)?.destinationConfig
                    updateFilteringConfiguration(destinationConfig)
                }
        }
    }

    private fun updateFilteringConfiguration(destinationConfig: JsonObject?) {
        val option = destinationConfig?.getString(EVENT_FILTERING_OPTION).orEmpty()

        if (option.isBlank()) {
            analytics.logger.debug("EventFilteringPlugin: Missing event filtering option for destination: $key")
            filteringRule = FilteringRule()
            return
        }

        filteringRule = FilteringRule(option, getEventFilteringList(option, destinationConfig).toSet())
    }

    private fun getEventFilteringList(eventFilteringOption: String, destinationConfig: JsonObject?): List<String> {
        val listKey = when (eventFilteringOption) {
            WHITE_LIST_EVENTS -> WHITE_LIST_EVENTS
            BLACK_LIST_EVENTS -> BLACK_LIST_EVENTS
            else -> return emptyList()
        }

        val serializedEvents = destinationConfig?.get(listKey)?.toString()

        if (serializedEvents.isNullOrBlank()) {
            analytics.logger.error("EventFilteringPlugin: Missing $listKey in destination config for: $key")
            return emptyList()
        }

        return parseFilteredEvents(serializedEvents)
    }

    private fun parseFilteredEvents(serializedEvents: String): List<String> {
        return try {
            LenientJson.decodeFromString<List<FilteredEvent>>(serializedEvents).mapNotNull {
                it.eventName.trim().takeIf { name -> name.isNotEmpty() }
            }
        } catch (e: IllegalArgumentException) {
            analytics.logger.error("EventFilteringPlugin: Error decoding event list: ${e.message}")
            emptyList()
        }
    }

    @Serializable
    data class FilteredEvent(
        val eventName: String,
    )

    private data class FilteringRule(
        val option: String = String.empty(),
        val events: Set<String> = emptySet(),
    )
}
