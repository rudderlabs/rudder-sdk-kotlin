package com.rudderstack.sdk.kotlin.core.plugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.queue.EventQueue
import com.rudderstack.sdk.kotlin.core.mockAnalytics
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RudderStackDataplanePluginTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var mockAnalytics: Analytics
    private lateinit var mockEventQueue: EventQueue
    private lateinit var plugin: RudderStackDataplanePlugin

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockAnalytics = mockAnalytics(testScope, testDispatcher)
        mockEventQueue = mockk(relaxed = true)

        plugin = spyk(RudderStackDataplanePlugin())

        plugin.setup(mockAnalytics)
        plugin.eventQueue = mockEventQueue
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `given a track event, when track is called, then the event is enqueued and returned correctly`() {
        val trackMessage = mockk<TrackEvent>(relaxed = true)

        val result = plugin.track(trackMessage)

        assertEquals(trackMessage, result)
        verify { mockEventQueue.put(trackMessage) }
    }

    @Test
    fun `given a plugin, when flush is executed, then verify that the message queue's flush method is called`() {
        plugin.flush()

        verify { mockEventQueue.flush() }
    }

    @Test
    fun `given a plugin, when teardown is executed, then verify that the message queue's stop method is called`() = runTest {
        plugin.teardown()

        advanceUntilIdle()

        coVerify { mockEventQueue.stop() }
    }
}
