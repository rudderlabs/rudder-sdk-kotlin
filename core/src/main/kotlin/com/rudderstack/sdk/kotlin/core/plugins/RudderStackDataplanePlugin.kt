package com.rudderstack.sdk.kotlin.core.plugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.plugins.EventPlugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.queue.EventQueue
import org.jetbrains.annotations.VisibleForTesting

internal class RudderStackDataplanePlugin : EventPlugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Terminal
    override lateinit var analytics: Analytics

    @VisibleForTesting
    internal var eventQueue: EventQueue? = null

    override fun track(payload: TrackEvent) {
        enqueue(payload)
    }

    override fun screen(payload: ScreenEvent) {
        enqueue(payload)
    }

    override fun group(payload: GroupEvent) {
        enqueue(payload)
    }

    override fun identify(payload: IdentifyEvent) {
        enqueue(payload)
    }

    override fun alias(payload: AliasEvent) {
        enqueue(payload)
    }

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        eventQueue = EventQueue(analytics).apply { start() }
    }

    internal fun flush() {
        eventQueue?.flush()
    }

    override fun teardown() {
        eventQueue?.stop()
    }

    private fun enqueue(event: Event) {
        this.eventQueue?.put(event)
    }
}
