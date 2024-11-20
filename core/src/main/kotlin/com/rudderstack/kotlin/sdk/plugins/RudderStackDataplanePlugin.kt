package com.rudderstack.kotlin.sdk.plugins

import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.AliasEvent
import com.rudderstack.kotlin.sdk.internals.models.FlushEvent
import com.rudderstack.kotlin.sdk.internals.models.GroupEvent
import com.rudderstack.kotlin.sdk.internals.models.IdentifyEvent
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.models.ScreenEvent
import com.rudderstack.kotlin.sdk.internals.models.TrackEvent
import com.rudderstack.kotlin.sdk.internals.plugins.MessagePlugin
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import com.rudderstack.kotlin.sdk.internals.queue.MessageQueue
import org.jetbrains.annotations.VisibleForTesting

internal class RudderStackDataplanePlugin : MessagePlugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Destination
    override lateinit var analytics: Analytics

    @VisibleForTesting
    internal var messageQueue: MessageQueue? = null

    override fun track(payload: TrackEvent): Message {
        enqueue(payload)
        return payload
    }

    override fun screen(payload: ScreenEvent): Message {
        enqueue(payload)
        return payload
    }

    override fun group(payload: GroupEvent): Message? {
        enqueue(payload)
        return payload
    }

    override fun identify(payload: IdentifyEvent): Message {
        enqueue(payload)
        return payload
    }

    override fun alias(payload: AliasEvent): Message {
        enqueue(payload)
        return payload
    }

    override fun flush(payload: FlushEvent): Message {
        enqueue(payload)
        return payload
    }

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        messageQueue = MessageQueue(analytics).apply { start() }
    }

    internal fun flush() {
        messageQueue?.flush()
    }

    override fun teardown() {
        messageQueue?.stop()
    }

    private fun enqueue(message: Message) {
        this.messageQueue?.put(message)
    }
}
