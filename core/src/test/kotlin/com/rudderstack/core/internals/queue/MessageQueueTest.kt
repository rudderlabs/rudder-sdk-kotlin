package com.rudderstack.core.internals.queue

import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.logger.Logger
import com.rudderstack.core.internals.models.Message
import com.rudderstack.core.internals.network.HttpClient
import com.rudderstack.core.internals.storage.Storage
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessageQueueTest {

    @MockK
    private lateinit var mockAnalytics: Analytics

    @MockK
    private lateinit var mockStorage: Storage

    @MockK
    private lateinit var mockHttpClient: HttpClient

    @MockK
    private lateinit var mockLogger: Logger

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var messageQueue: MessageQueue

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        every { mockAnalytics.configuration.storageProvider } returns mockStorage
        every { mockAnalytics.configuration.logger } returns mockLogger
        every { mockAnalytics.analyticsScope } returns CoroutineScope(testDispatcher)
        every { mockAnalytics.storageDispatcher } returns testDispatcher
        every { mockAnalytics.networkDispatcher } returns testDispatcher

        messageQueue = spyk(MessageQueue(mockAnalytics, mockHttpClient))
    }

    @After
    fun tearDown() {
        messageQueue.stop()
    }

    @Test
    fun `given a message queue, when queue starts, then verify channel running is true`() {
        messageQueue.start()
        assertTrue(messageQueue::class.java.getDeclaredField("running").apply {
            isAccessible = true
        }.getBoolean(messageQueue))
    }

    @Test
    fun `given a message queue, when queue stops, then cancels channels and sets running to false`() {
        messageQueue.start()
        messageQueue.stop()

        val running =
            messageQueue::class.java.getDeclaredField("running").apply { isAccessible = true }
                .getBoolean(messageQueue)
        assertTrue(!running)
        assertTrue(
            messageQueue::class.java.getDeclaredField("writeChannel").apply { isAccessible = true }
                .get(messageQueue) is Channel<*>)
        assertTrue(
            messageQueue::class.java.getDeclaredField("uploadChannel").apply { isAccessible = true }
                .get(messageQueue) is Channel<*>)
    }

    @Test
    fun `given a message, when stringifyBaseEvent is called, then the expected JSON string is returned`() {
        val message = mockk<Message>(relaxed = true)
        val jsonString = """{"type":"track","event":"Test Event"}"""
        every { messageQueue.stringifyBaseEvent(message) } returns jsonString

        val result = messageQueue.stringifyBaseEvent(message)
        assertEquals(jsonString, result)
    }

    @Test
    fun `test readFileAsString reads file correctly`() {
        val filePath = "test_file_path"
        val fileContent = "test content"

        every { messageQueue.readFileAsString(filePath) } returns fileContent

        val result =
            messageQueue::class.java.getDeclaredMethod("readFileAsString", String::class.java)
                .apply { isAccessible = true }
                .invoke(messageQueue, filePath)

        assertEquals(fileContent, result)
    }
}
