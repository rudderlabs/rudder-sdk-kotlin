package com.rudderstack.kotlin.internals.plugins

import com.rudderstack.kotlin.internals.models.FlushEvent
import com.rudderstack.kotlin.internals.models.GroupEvent
import com.rudderstack.kotlin.internals.models.Message
import com.rudderstack.kotlin.internals.models.ScreenEvent
import com.rudderstack.kotlin.internals.models.TrackEvent

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

    fun flush(payload: FlushEvent): Message {
        return payload
    }

    override fun execute(message: Message): Message? = when (message) {
        is TrackEvent -> track(message)
        is ScreenEvent -> screen(message)
        is GroupEvent -> group(message)
        is FlushEvent -> flush(message)
    }
}
