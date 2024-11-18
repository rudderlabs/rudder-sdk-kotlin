package com.rudderstack.kotlin.sdk.plugins

import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.models.TrackEvent
import com.rudderstack.kotlin.sdk.internals.queue.MessageQueue
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RudderStackDataplanePluginTest {

    private lateinit var analytics: Analytics
    private lateinit var messageQueue: MessageQueue
    private lateinit var plugin: RudderStackDataplanePlugin

    @Before
    fun setUp() {
        analytics = mockk(relaxed = true)
        messageQueue = mockk(relaxed = true)

        plugin = spyk(RudderStackDataplanePlugin(), recordPrivateCalls = true)

        plugin.analytics = analytics

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
    fun `given a plugin, when flush is executed, then verify that the message queue's flush method is called`() {
        plugin.flush()

        verify { messageQueue.flush() }
    }
}
