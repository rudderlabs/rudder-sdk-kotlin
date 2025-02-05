package com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing

import com.rudderstack.sdk.kotlin.android.utils.findDestination
import com.rudderstack.sdk.kotlin.android.utils.getString
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.CopyOnWriteArrayList

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
    private var filteringOption = String.empty()
    private val filteringList = CopyOnWriteArrayList<String>()

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
                LoggerAnalytics.debug("EventFilteringPlugin: Dropped event '$eventName' for destination: $key")
                null
            }
            else -> event
        }
    }

    override fun teardown() {
        filteringList.clear()
    }

    private fun shouldDropEvent(eventName: String): Boolean {
        return when (filteringOption) {
            WHITE_LIST_EVENTS -> eventName !in filteringList
            BLACK_LIST_EVENTS -> eventName in filteringList
            else -> false
        }
    }

    private fun listenForConfigChanges() {
        // todo: use integrations dispatcher here and then we don't need to use CopyOnWriteArrayList for filteringList
        analytics.analyticsScope.launch {
            analytics.sourceConfigState
                .filter { it.source.isSourceEnabled }
                .collect { sourceConfig ->
                    val destinationConfig = findDestination(sourceConfig, key)?.destinationConfig
                    updateFilteringConfiguration(destinationConfig)
                }
        }
    }

    private fun updateFilteringConfiguration(destinationConfig: JsonObject?) {
        filteringOption = destinationConfig?.getString(EVENT_FILTERING_OPTION).orEmpty()
        filteringList.clear()

        if (filteringOption.isBlank()) {
            LoggerAnalytics.error("EventFilteringPlugin: Missing event filtering option for destination: $key")
            return
        }

        filteringList.addAll(getEventFilteringList(filteringOption, destinationConfig))
    }

    private fun getEventFilteringList(eventFilteringOption: String, destinationConfig: JsonObject?): List<String> {
        val listKey = when (eventFilteringOption) {
            WHITE_LIST_EVENTS -> WHITE_LIST_EVENTS
            BLACK_LIST_EVENTS -> BLACK_LIST_EVENTS
            else -> return emptyList()
        }

        val serializedEvents = destinationConfig?.get(listKey)?.toString()

        if (serializedEvents.isNullOrBlank()) {
            LoggerAnalytics.error("EventFilteringPlugin: Missing $listKey in destination config for: $key")
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
            LoggerAnalytics.error("EventFilteringPlugin: Error decoding event list: ${e.message}")
            emptyList()
        }
    }

    @Serializable
    data class FilteredEvent(
        val eventName: String,
    )
}
