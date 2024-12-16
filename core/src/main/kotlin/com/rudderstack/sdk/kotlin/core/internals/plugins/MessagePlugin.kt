package com.rudderstack.sdk.kotlin.core.internals.plugins

import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent

internal interface MessagePlugin : Plugin {
    fun track(payload: TrackEvent): Event? {
        return payload
    }

    fun screen(payload: ScreenEvent): Event? {
        return payload
    }

    fun group(payload: GroupEvent): Event? {
        return payload
    }

    fun identify(payload: IdentifyEvent): Event {
        return payload
    }

    fun alias(payload: AliasEvent): Event {
        return payload
    }

    override suspend fun intercept(event: Event): Event? = when (event) {
        is TrackEvent -> track(event)
        is ScreenEvent -> screen(event)
        is GroupEvent -> group(event)
        is IdentifyEvent -> identify(event)
        is AliasEvent -> alias(event)
    }
}
