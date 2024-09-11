package com.rudderstack.core.internals.queue

import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.logger.Logger
import com.rudderstack.core.internals.models.Message
import com.rudderstack.core.internals.network.ErrorStatus
import com.rudderstack.core.internals.network.HttpClient
import com.rudderstack.core.internals.network.Result
import com.rudderstack.core.internals.storage.Storage
import com.rudderstack.core.internals.storage.StorageKeys
import com.rudderstack.core.internals.utils.empty
import com.rudderstack.core.utils.mockAnalytics
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException

class MessageQueueTest {


    @MockK
    private lateinit var mockStorage: Storage

    @MockK
    private lateinit var mockHttpClient: HttpClient

    @MockK
    private lateinit var mockLogger: Logger

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics: Analytics = mockAnalytics(testScope, testDispatcher)

    private lateinit var messageQueue: MessageQueue

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        every { mockAnalytics.configuration.storageProvider } returns mockStorage
        every { mockAnalytics.configuration.logger } returns mockLogger

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

    @Test
    fun `given multiple batch is ready to be sent to the server and server returns success, when flush is called, then all the batches are sent to the server and removed from the storage`() =
        runTest {
            val storage = mockAnalytics.configuration.storageProvider
            // Two batch files are ready to be sent
            val filePaths = listOf(
                "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
                "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
            )
            val fileUrlList = filePaths.joinToString(",")

            // Mock storage read
            coEvery {
                storage.readString(StorageKeys.RUDDER_MESSAGE, String.empty())
            } returns fileUrlList

            // Mock file existence check
            every { messageQueue.isFileExists(any()) } returns true

            val batchPayload = "test content"

            // Mock messageQueue file reading
            filePaths.forEach { path ->
                every { messageQueue.readFileAsString(path) } returns batchPayload
            }

            // Mock the behavior for HttpClient
            every { mockHttpClient.sendData(batchPayload) } returns Result.Success("Ok")

            // Execute messageQueue actions
            messageQueue.start()
            messageQueue.flush()
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify the expected behavior
            filePaths.forEach { path ->
                verify(exactly = 1) { storage.remove(path) }
            }
        }

    @Test
    fun `given batch is ready to be sent to the server and server returns error, when flush is called, then the batch is not removed from storage`() {
        val storage = mockAnalytics.configuration.storageProvider
        // Two batch files are ready to be sent
        val filePaths = listOf(
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
        )
        val fileUrlList = filePaths.joinToString(",")

        // Mock storage read
        coEvery {
            storage.readString(StorageKeys.RUDDER_MESSAGE, String.empty())
        } returns fileUrlList

        // Mock file existence check
        every { messageQueue.isFileExists(any()) } returns true

        val batchPayload = "test content"

        // Mock messageQueue file reading
        filePaths.forEach { path ->
            every { messageQueue.readFileAsString(path) } returns batchPayload
        }

        // Mock the behavior for HttpClient
        every { mockHttpClient.sendData(batchPayload) } returns Result.Failure(
            ErrorStatus.GENERAL_ERROR,
            IOException("Internal Server Error")
        )

        // Execute messageQueue actions
        messageQueue.start()
        messageQueue.flush()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify the expected behavior
        filePaths.forEach { path ->
            verify(exactly = 0) { storage.remove(path) }
        }
    }

    @Test
    fun `given batch is ready to be sent to the server and file is not found, when flush is called, then the exception is thrown and handled`() {
        val storage = mockAnalytics.configuration.storageProvider
        // Two batch files are ready to be sent
        val filePaths = listOf(
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
        )
        val fileUrlList = filePaths.joinToString(",")

        // Mock storage read
        coEvery {
            storage.readString(StorageKeys.RUDDER_MESSAGE, String.empty())
        } returns fileUrlList

        // Mock file existence check
        every { messageQueue.isFileExists(any()) } returns true

        // Throw file not found exception while reading the file
        val exception = FileNotFoundException("File not found")
        filePaths.forEach { path ->
            every { messageQueue.readFileAsString(path) } throws exception
        }

        // Execute messageQueue actions
        messageQueue.start()
        messageQueue.flush()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = filePaths.size) {
            mockLogger.error(
                tag = "Rudder-Analytics",
                log = "Message storage file not found",
                exception
            )
        }
    }

    @Test
    fun `given batch is ready to be sent to the server and some exception occurs while reading the file, when flush is called, then the exception is thrown and handled`() {
        val storage = mockAnalytics.configuration.storageProvider
        // Two batch files are ready to be sent
        val filePaths = listOf(
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
        )
        val fileUrlList = filePaths.joinToString(",")

        // Mock storage read
        coEvery {
            storage.readString(StorageKeys.RUDDER_MESSAGE, String.empty())
        } returns fileUrlList

        // Mock file existence check
        every { messageQueue.isFileExists(any()) } returns true

        // Throw generic exception while reading the file
        val exception = Exception("File not found")
        filePaths.forEach { path ->
            every { messageQueue.readFileAsString(path) } throws exception
        }

        // Execute messageQueue actions
        messageQueue.start()
        messageQueue.flush()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = filePaths.size) {
            mockLogger.error(
                tag = "Rudder-Analytics",
                log = "Error when uploading event",
                exception
            )
        }
    }
}
