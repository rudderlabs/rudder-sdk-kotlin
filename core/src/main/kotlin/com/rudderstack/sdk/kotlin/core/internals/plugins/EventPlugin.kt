package com.rudderstack.sdk.kotlin.core.internals.plugins

import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi

/**
 * Advanced plugin that can act on specific event payloads.
 */
@InternalRudderApi
interface EventPlugin : Plugin {

    /**
     * Handles a track event.
     */
    fun track(payload: TrackEvent) {}

    /**
     * Handles a screen event.
     */
    fun screen(payload: ScreenEvent) {}

    /**
     * Handles a group event.
     */
    fun group(payload: GroupEvent) {}

    /**
     * Handles an identify event.
     */
    fun identify(payload: IdentifyEvent) {}

    /**
     * Handles an alias event.
     */
    fun alias(payload: AliasEvent) {}

    override suspend fun intercept(event: Event): Event? {
        handleEvent(event)
        return event
    }

    /**
     * Executes the appropriate method for the given event.
     */
    fun handleEvent(event: Event) = when (event) {
        is TrackEvent -> track(event)
        is ScreenEvent -> screen(event)
        is GroupEvent -> group(event)
        is IdentifyEvent -> identify(event)
        is AliasEvent -> alias(event)
    }
}
