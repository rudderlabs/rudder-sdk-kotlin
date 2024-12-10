package com.rudderstack.kotlin.core.internals.queue

import com.rudderstack.kotlin.core.Analytics
import com.rudderstack.kotlin.core.internals.logger.KotlinLogger
import com.rudderstack.kotlin.core.internals.models.Message
import com.rudderstack.kotlin.core.internals.models.provider.provideEvent
import com.rudderstack.kotlin.core.internals.network.ErrorStatus
import com.rudderstack.kotlin.core.internals.network.HttpClient
import com.rudderstack.kotlin.core.internals.utils.Result
import com.rudderstack.kotlin.core.internals.policies.FlushPoliciesFacade
import com.rudderstack.kotlin.core.internals.storage.Storage
import com.rudderstack.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.kotlin.core.internals.utils.empty
import com.rudderstack.kotlin.core.internals.utils.encodeToString
import com.rudderstack.kotlin.core.mockAnalytics
import com.rudderstack.kotlin.core.setupLogger
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
import java.io.FileNotFoundException
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MessageQueueTest {

    @MockK
    private lateinit var mockStorage: Storage

    @MockK
    private lateinit var mockHttpClient: HttpClient

    @MockK
    private lateinit var mockKotlinLogger: KotlinLogger

    @MockK
    private lateinit var mockFlushPoliciesFacade: FlushPoliciesFacade

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics: Analytics = mockAnalytics(testScope, testDispatcher)

    private lateinit var messageQueue: MessageQueue

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)

        setupLogger(mockKotlinLogger)

        every { mockAnalytics.configuration.storage } returns mockStorage

        coEvery { mockStorage.close() } just runs
        coEvery { mockStorage.write(StorageKeys.MESSAGE, any<String>()) } just runs

        messageQueue = spyk(
            MessageQueue(
                analytics = mockAnalytics,
                httpClientFactory = mockHttpClient,
                flushPoliciesFacade = mockFlushPoliciesFacade,
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `given a message queue is accepting events, when message is sent, then it should be stored`() = runTest {
        val message = provideEvent()
        messageQueue.start()

        messageQueue.put(message)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.MESSAGE, message.encodeToString())
        }
    }

    @Test
    fun `given a message queue is accepting events, when queue stops, then it should stop storing new events`() = runTest {
        val message = provideEvent()
        messageQueue.start()

        messageQueue.stop()
        advanceUntilIdle()
        messageQueue.put(message)
        advanceUntilIdle()

        coVerify(exactly = 0) {
            mockStorage.write(StorageKeys.MESSAGE, message.encodeToString())
        }
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

        val result = messageQueue.readFileAsString(filePath)

        assertEquals(fileContent, result)
    }

    @Test
    fun `given multiple batch is ready to be sent to the server and server returns success, when flush is called, then all the batches are sent to the server and removed from the storage`() =
        runTest {
            val storage = mockAnalytics.configuration.storage
            // Two batch files are ready to be sent
            val filePaths = listOf(
                "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
                "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
            )
            val fileUrlList = filePaths.joinToString(",")

            // Mock storage read
            coEvery {
                storage.readString(StorageKeys.MESSAGE, String.empty())
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
        val storage = mockAnalytics.configuration.storage
        // Two batch files are ready to be sent
        val filePaths = listOf(
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
        )
        val fileUrlList = filePaths.joinToString(",")

        // Mock storage read
        coEvery {
            storage.readString(StorageKeys.MESSAGE, String.empty())
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
        val storage = mockAnalytics.configuration.storage
        // Two batch files are ready to be sent
        val filePaths = listOf(
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
        )
        val fileUrlList = filePaths.joinToString(",")

        // Mock storage read
        coEvery {
            storage.readString(StorageKeys.MESSAGE, String.empty())
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
            mockKotlinLogger.error("Message storage file not found", exception)
        }
    }

    @Test
    fun `given batch is ready to be sent to the server and some exception occurs while reading the file, when flush is called, then the exception is thrown and handled`() {
        val storage = mockAnalytics.configuration.storage
        // Two batch files are ready to be sent
        val filePaths = listOf(
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
        )
        val fileUrlList = filePaths.joinToString(",")

        // Mock storage read
        coEvery {
            storage.readString(StorageKeys.MESSAGE, String.empty())
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
            mockKotlinLogger.error("Error when uploading event", exception)
        }
    }

    @Test
    fun `given default flush policies are enabled, when message queue is started, then flush policies should be scheduled`() {
        messageQueue.start()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) {
            mockFlushPoliciesFacade.schedule(mockAnalytics)
        }
    }

    @Test
    fun `given default flush policies are enabled, when first event is made, then flush call should be triggered`() {
        val storage = mockAnalytics.configuration.storage
        val mockMessage: Message = mockk(relaxed = true)
        val jsonString = """{"type":"track","event":"Test Event"}"""
        every { messageQueue.stringifyBaseEvent(mockMessage) } returns jsonString
        // Mock the behavior for StartupFlushPolicy
        every { mockFlushPoliciesFacade.shouldFlush() } returns true

        // Execute messageQueue actions
        messageQueue.start()
        messageQueue.put(mockMessage)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            mockFlushPoliciesFacade.reset()
            storage.rollover()
        }
    }

    @Test
    fun `given default flush policies are enabled, when 30 events are made, then flush call should be triggered`() {
        val storage = mockAnalytics.configuration.storage
        val mockMessage: Message = mockk(relaxed = true)
        val jsonString = """{"type":"track","event":"Test Event"}"""
        every { messageQueue.stringifyBaseEvent(mockMessage) } returns jsonString
        // Mock the behavior for StartupFlushPolicy
        every { mockFlushPoliciesFacade.shouldFlush() } returns true

        // Execute messageQueue actions
        messageQueue.start()

        // Make the first event
        messageQueue.put(mockMessage)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            mockFlushPoliciesFacade.reset()
            storage.rollover()
        }

        // Mock the behavior for CountFlushPolicy
        every { mockFlushPoliciesFacade.shouldFlush() } returns false

        repeat(29) {
            messageQueue.put(mockMessage)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // No new flush should be triggered
        coVerify(exactly = 1) {
            mockFlushPoliciesFacade.reset()
            storage.rollover()
        }

        // Mock the behavior for CountFlushPolicy
        every { mockFlushPoliciesFacade.shouldFlush() } returns true

        // Make the 30th event
        messageQueue.put(mockMessage)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 2) {
            mockFlushPoliciesFacade.reset()
            storage.rollover()
        }
    }

    @Test
    fun `given default flush policies are enabled, when events are made, then the flush policies state should be updated`() {
        val storage = mockAnalytics.configuration.storage
        val times = 20
        val mockMessage: Message = mockk(relaxed = true)
        val jsonString = """{"type":"track","event":"Test Event"}"""
        every { messageQueue.stringifyBaseEvent(mockMessage) } returns jsonString

        // Execute messageQueue actions
        messageQueue.start()

        repeat(times) {
            messageQueue.put(mockMessage)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = times) {
            storage.write(StorageKeys.MESSAGE, jsonString)
            mockFlushPoliciesFacade.updateState()
        }
    }

    @Test
    fun `given default flush policies are enabled, when flush is called, then flush policies should be reset`() {
        messageQueue.start()
        messageQueue.flush()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) {
            mockFlushPoliciesFacade.reset()
        }
    }

    @Test
    fun `given default flush policies are enabled, when stop is called, then flush policies should be cancelled`() =
        runTest {
            messageQueue.start()

            messageQueue.stop()
            testDispatcher.scheduler.advanceUntilIdle()

            verify(exactly = 1) {
                mockFlushPoliciesFacade.cancelSchedule()
            }
        }

    @Test
    fun `given no policies are enabled, when explicit flush call is made, then rollover should happen`() {
        val storage = mockAnalytics.configuration.storage
        val times = 100
        val mockMessage: Message = mockk(relaxed = true)
        val jsonString = """{"type":"track","event":"Test Event"}"""
        every { messageQueue.stringifyBaseEvent(mockMessage) } returns jsonString
        // Mock the behavior when no flush policies are enabled
        every { mockFlushPoliciesFacade.shouldFlush() } returns false

        // Execute messageQueue actions
        messageQueue.start()
        repeat(times) {
            messageQueue.put(mockMessage)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) {
            storage.rollover()
        }

        messageQueue.flush()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            storage.rollover()
        }
    }
}
