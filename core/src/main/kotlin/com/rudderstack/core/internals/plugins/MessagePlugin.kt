package com.rudderstack.core.internals.plugins

import com.rudderstack.core.internals.models.FlushMessage
import com.rudderstack.core.internals.models.Message
import com.rudderstack.core.internals.models.TrackEvent

interface MessagePlugin : Plugin {
    fun track(payload: TrackEvent): Message? {
        return payload
    }

    fun flush(payload: FlushMessage): Message {
        return payload
    }

    override fun execute(message: Message): Message? = when (message) {
        is TrackEvent -> track(message)
        is FlushMessage -> flush(message)
    }
}