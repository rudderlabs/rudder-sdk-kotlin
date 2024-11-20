package com.rudderstack.kotlin.sdk.internals.plugins

import com.rudderstack.kotlin.sdk.internals.models.AliasEvent
import com.rudderstack.kotlin.sdk.internals.models.GroupEvent
import com.rudderstack.kotlin.sdk.internals.models.IdentifyEvent
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.models.ScreenEvent
import com.rudderstack.kotlin.sdk.internals.models.TrackEvent

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

    override suspend fun execute(message: Message): Message? = when (message) {
        is TrackEvent -> track(message)
        is ScreenEvent -> screen(message)
        is GroupEvent -> group(message)
        is IdentifyEvent -> identify(message)
        is AliasEvent -> alias(message)
    }
}
