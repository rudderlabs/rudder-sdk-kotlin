package com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing

import com.rudderstack.sdk.kotlin.android.utils.getString
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

private const val WHITE_LIST_EVENTS = "whitelistedEvents"
private const val BLACK_LIST_EVENTS = "blacklistedEvents"
private const val EVENT_FILTERING_OPTION = "eventFilteringOption"

internal class EventFilteringPlugin(private val destinationConfig: JsonObject) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    private val filteringOption = destinationConfig.getString(EVENT_FILTERING_OPTION)
    private val filteringList = getEventFilteringList(filteringOption)

    override suspend fun intercept(event: Event): Event? {
        if (event !is TrackEvent) {
            return event
        }

        return when (filteringOption) {
            WHITE_LIST_EVENTS -> {
                if (filteringList.contains(event.event)) {
                    event
                } else {
                    null
                }
            }

            BLACK_LIST_EVENTS -> {
                if (!filteringList.contains(event.event)) {
                    event
                } else {
                    null
                }
            }

            else -> event
        }
    }

    private fun getEventFilteringList(eventFilteringOption: String?): List<String> {
        val stringifiedFilteredEvents = when (eventFilteringOption) {
            WHITE_LIST_EVENTS -> destinationConfig[WHITE_LIST_EVENTS]?.toString()
            BLACK_LIST_EVENTS -> destinationConfig[BLACK_LIST_EVENTS]?.toString()
            else -> null
        }
        return stringifiedFilteredEvents
            ?.let { LenientJson.decodeFromString<List<FilteredEvent>>(it) }
            ?.map { it.eventName }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }

    @Serializable
    data class FilteredEvent(
        val eventName: String,
    )
}
