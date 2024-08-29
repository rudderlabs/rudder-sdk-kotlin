package com.rudderstack.core.internals.plugins

import com.rudderstack.core.internals.models.FlushMessage
import com.rudderstack.core.internals.models.Message
import com.rudderstack.core.internals.models.TrackMessage

interface MessagePlugin : Plugin {
    fun track(payload: TrackMessage): Message? {
        return payload
    }

    fun flush(payload: FlushMessage): Message {
        return payload
    }

    override fun execute(message: Message): Message? = when (message) {
        is TrackMessage -> track(message)
        is FlushMessage -> flush(message)
    }
}