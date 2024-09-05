package com.rudderstack.core.plugins

import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.models.FlushMessage
import com.rudderstack.core.internals.models.TrackEvent
import com.rudderstack.core.internals.queue.MessageQueue
import com.rudderstack.core.internals.plugins.Plugin
import com.rudderstack.core.internals.plugins.PluginChain
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DataplanePluginTest {

    private lateinit var analytics: Analytics
    private lateinit var messageQueue: MessageQueue
    private lateinit var pluginChain: PluginChain
    private lateinit var plugin: DataplanePlugin

    @Before
    fun setUp() {
        analytics = mockk(relaxed = true)
        messageQueue = mockk(relaxed = true)
        pluginChain = mockk(relaxed = true)

        plugin = spyk(DataplanePlugin(), recordPrivateCalls = true)

        plugin.analytics = analytics
        pluginChain.analytics = analytics
        plugin::class.java.getDeclaredField("pluginChain").apply {
            isAccessible = true
            set(plugin, pluginChain)
        }

        plugin::class.java.getDeclaredField("messageQueue").apply {
            isAccessible = true
            set(plugin, messageQueue)
        }
    }

    @Test
    fun `given a track event, when track is called, then the event is enqueued and returned correctly`() {
        val trackMessage = mockk<TrackEvent>(relaxed = true)

        val result = plugin.track(trackMessage)

        assertEquals(trackMessage, result)
        verify { messageQueue.put(trackMessage) }
    }

    @Test
    fun `given a flush event, when flush is called, test flush is enqueued and returned correctly`() {
        val flushMessage = mockk<FlushMessage>(relaxed = true)

        val result = plugin.flush(flushMessage)

        assertEquals(flushMessage, result)
        verify { messageQueue.put(flushMessage) }
    }

    @Test
    fun `given a plugin, when a plugin is removed from the chain, then verify that remove method is called`() {
        val pluginToRemove = mockk<Plugin>(relaxed = true)

        plugin.remove(pluginToRemove)

        verify { pluginChain.remove(pluginToRemove) }
    }

    @Test
    fun `given a plugin, when flush is executed, then verify that the message queue's flush method is called`() {
        plugin.flush()

        verify { messageQueue.flush() }
    }
}
