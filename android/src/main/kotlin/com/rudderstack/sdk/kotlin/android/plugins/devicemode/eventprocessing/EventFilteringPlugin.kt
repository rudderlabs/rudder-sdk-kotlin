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
        analytics.analyticsScope.launch {
            analytics.sourceConfigState
                .filter { it.source.isSourceEnabled }
                .collect { sourceConfig ->
                    val destinationConfig = findDestination(sourceConfig, key)?.destinationConfig

                    filteringOption = destinationConfig
                        ?.getString(EVENT_FILTERING_OPTION)
                        ?.also { option ->
                            filteringList.clear()
                            filteringList.addAll(getEventFilteringList(option, destinationConfig))
                        } ?: run {
                        LoggerAnalytics.error(
                            "EventFilteringPlugin: Event filtering option not found " +
                                "in destination config for destination: $key"
                        )
                        String.empty()
                    }
                }
        }
    }

    override suspend fun intercept(event: Event): Event? {
        if (event !is TrackEvent) {
            return event
        }

        return when (filteringOption) {
            WHITE_LIST_EVENTS -> {
                if (filteringList.contains(event.event.trim())) {
                    event
                } else {
                    null
                }
            }

            BLACK_LIST_EVENTS -> {
                if (!filteringList.contains(event.event.trim())) {
                    event
                } else {
                    null
                }
            }

            else -> event
        }
    }

    override fun teardown() {
        filteringList.clear()
    }

    private fun getEventFilteringList(eventFilteringOption: String, destinationConfig: JsonObject): List<String> {
        val stringifiedFilteredEvents = when (eventFilteringOption) {
            WHITE_LIST_EVENTS -> destinationConfig[WHITE_LIST_EVENTS]
                ?.toString()
                .also {
                    if (it == null) {
                        LoggerAnalytics.error(
                            "EventFilteringPlugin: Whitelisted events not found " +
                                "in destination config for destination: $key"
                        )
                    }
                }

            BLACK_LIST_EVENTS -> destinationConfig[BLACK_LIST_EVENTS]
                ?.toString()
                .also {
                    if (it == null) {
                        LoggerAnalytics.error(
                            "EventFilteringPlugin: Blacklisted events not found " +
                                "in destination config for destination: $key"
                        )
                    }
                }

            else -> null
        }

        return stringifiedFilteredEvents
            ?.let {
                try {
                    LenientJson.decodeFromString<List<FilteredEvent>>(it)
                } catch (e: IllegalArgumentException) {
                    LoggerAnalytics.error("EventFilteringPlugin: Error decoding filtered events list: ${e.message}")
                    null
                }
            }
            ?.map { it.eventName.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }

    @Serializable
    data class FilteredEvent(
        val eventName: String,
    )
}
