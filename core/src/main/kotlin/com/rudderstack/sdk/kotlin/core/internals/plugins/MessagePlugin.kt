package com.rudderstack.sdk.kotlin.core.internals.plugins

import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Message
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent

internal interface MessagePlugin : Plugin {
    fun track(payload: TrackEvent): Message? {
        return payload
    }

    fun screen(payload: ScreenEvent): Message? {
        return payload
    }

    fun group(payload: GroupEvent): Message? {
        return payload
    }

    fun identify(payload: IdentifyEvent): Message {
        return payload
    }

    fun alias(payload: AliasEvent): Message {
        return payload
    }

    override suspend fun intercept(message: Message): Message? = when (message) {
        is TrackEvent -> track(message)
        is ScreenEvent -> screen(message)
        is GroupEvent -> group(message)
        is IdentifyEvent -> identify(message)
        is AliasEvent -> alias(message)
    }
}
