package com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing

import com.rudderstack.sdk.kotlin.core.internals.models.Destination
import com.rudderstack.sdk.kotlin.core.internals.models.Event

internal class EventProcessorFacade(
    private val eventProcessors: List<EventProcessor>
) {

    fun process(event: Event, key: String, destination: Destination): Event? {
        var processedEvent = event.copy<Event>()
        eventProcessors.forEach {
            processedEvent = it.process(processedEvent, key, destination) ?: return null
        }
        return processedEvent
    }
}
