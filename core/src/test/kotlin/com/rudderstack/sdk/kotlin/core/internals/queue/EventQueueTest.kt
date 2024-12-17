package com.rudderstack.sdk.kotlin.core.internals.queue

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.KotlinLogger
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.provider.provideEvent
import com.rudderstack.sdk.kotlin.core.internals.network.ErrorStatus
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClient
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.policies.FlushPoliciesFacade
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToString
import com.rudderstack.sdk.kotlin.core.mockAnalytics
import com.rudderstack.sdk.kotlin.core.setupLogger
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
class EventQueueTest {

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

    private lateinit var eventQueue: EventQueue

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)

        setupLogger(mockKotlinLogger)

        every { mockAnalytics.storage } returns mockStorage

        coEvery { mockStorage.close() } just runs
        coEvery { mockStorage.write(StorageKeys.EVENT, any<String>()) } just runs

        eventQueue = spyk(
            EventQueue(
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
        eventQueue.start()

        eventQueue.put(message)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, message.encodeToString())
        }
    }

    @Test
    fun `given a message queue is accepting events, when queue stops, then it should stop storing new events`() = runTest {
        val message = provideEvent()
        eventQueue.start()

        eventQueue.stop()
        advanceUntilIdle()
        eventQueue.put(message)
        advanceUntilIdle()

        coVerify(exactly = 0) {
            mockStorage.write(StorageKeys.EVENT, message.encodeToString())
        }
    }

    @Test
    fun `given a message, when stringifyBaseEvent is called, then the expected JSON string is returned`() {
        val event = mockk<Event>(relaxed = true)
        val jsonString = """{"type":"track","event":"Test Event"}"""
        every { eventQueue.stringifyBaseEvent(event) } returns jsonString

        val result = eventQueue.stringifyBaseEvent(event)
        assertEquals(jsonString, result)
    }

    @Test
    fun `test readFileAsString reads file correctly`() {
        val filePath = "test_file_path"
        val fileContent = "test content"

        every { eventQueue.readFileAsString(filePath) } returns fileContent

        val result = eventQueue.readFileAsString(filePath)

        assertEquals(fileContent, result)
    }

    @Test
    fun `given multiple batch is ready to be sent to the server and server returns success, when flush is called, then all the batches are sent to the server and removed from the storage`() =
        runTest {
            val storage = mockAnalytics.storage
            // Two batch files are ready to be sent
            val filePaths = listOf(
                "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
                "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
            )
            val fileUrlList = filePaths.joinToString(",")

            // Mock storage read
            coEvery {
                storage.readString(StorageKeys.EVENT, String.empty())
            } returns fileUrlList

            // Mock file existence check
            every { eventQueue.isFileExists(any()) } returns true

            val batchPayload = "test content"

            // Mock messageQueue file reading
            filePaths.forEach { path ->
                every { eventQueue.readFileAsString(path) } returns batchPayload
            }

            // Mock the behavior for HttpClient
            every { mockHttpClient.sendData(batchPayload) } returns Result.Success("Ok")

            // Execute messageQueue actions
            eventQueue.start()
            eventQueue.flush()
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify the expected behavior
            filePaths.forEach { path ->
                verify(exactly = 1) { storage.remove(path) }
            }
        }

    @Test
    fun `given batch is ready to be sent to the server and server returns error, when flush is called, then the batch is not removed from storage`() {
        val storage = mockAnalytics.storage
        // Two batch files are ready to be sent
        val filePaths = listOf(
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
        )
        val fileUrlList = filePaths.joinToString(",")

        // Mock storage read
        coEvery {
            storage.readString(StorageKeys.EVENT, String.empty())
        } returns fileUrlList

        // Mock file existence check
        every { eventQueue.isFileExists(any()) } returns true

        val batchPayload = "test content"

        // Mock messageQueue file reading
        filePaths.forEach { path ->
            every { eventQueue.readFileAsString(path) } returns batchPayload
        }

        // Mock the behavior for HttpClient
        every { mockHttpClient.sendData(batchPayload) } returns Result.Failure(
            ErrorStatus.GENERAL_ERROR,
            IOException("Internal Server Error")
        )

        // Execute messageQueue actions
        eventQueue.start()
        eventQueue.flush()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify the expected behavior
        filePaths.forEach { path ->
            verify(exactly = 0) { storage.remove(path) }
        }
    }

    @Test
    fun `given batch is ready to be sent to the server and file is not found, when flush is called, then the exception is thrown and handled`() {
        val storage = mockAnalytics.storage
        // Two batch files are ready to be sent
        val filePaths = listOf(
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
        )
        val fileUrlList = filePaths.joinToString(",")

        // Mock storage read
        coEvery {
            storage.readString(StorageKeys.EVENT, String.empty())
        } returns fileUrlList

        // Mock file existence check
        every { eventQueue.isFileExists(any()) } returns true

        // Throw file not found exception while reading the file
        val exception = FileNotFoundException("File not found")
        filePaths.forEach { path ->
            every { eventQueue.readFileAsString(path) } throws exception
        }

        // Execute messageQueue actions
        eventQueue.start()
        eventQueue.flush()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = filePaths.size) {
            mockKotlinLogger.error("Message storage file not found", exception)
        }
    }

    @Test
    fun `given batch is ready to be sent to the server and some exception occurs while reading the file, when flush is called, then the exception is thrown and handled`() {
        val storage = mockAnalytics.storage
        // Two batch files are ready to be sent
        val filePaths = listOf(
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
        )
        val fileUrlList = filePaths.joinToString(",")

        // Mock storage read
        coEvery {
            storage.readString(StorageKeys.EVENT, String.empty())
        } returns fileUrlList

        // Mock file existence check
        every { eventQueue.isFileExists(any()) } returns true

        // Throw generic exception while reading the file
        val exception = Exception("File not found")
        filePaths.forEach { path ->
            every { eventQueue.readFileAsString(path) } throws exception
        }

        // Execute messageQueue actions
        eventQueue.start()
        eventQueue.flush()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = filePaths.size) {
            mockKotlinLogger.error("Error when uploading event", exception)
        }
    }

    @Test
    fun `given default flush policies are enabled, when message queue is started, then flush policies should be scheduled`() {
        eventQueue.start()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) {
            mockFlushPoliciesFacade.schedule(mockAnalytics)
        }
    }

    @Test
    fun `given default flush policies are enabled, when first event is made, then flush call should be triggered`() {
        val storage = mockAnalytics.storage
        val mockEvent: Event = mockk(relaxed = true)
        val jsonString = """{"type":"track","event":"Test Event"}"""
        every { eventQueue.stringifyBaseEvent(mockEvent) } returns jsonString
        // Mock the behavior for StartupFlushPolicy
        every { mockFlushPoliciesFacade.shouldFlush() } returns true

        // Execute messageQueue actions
        eventQueue.start()
        eventQueue.put(mockEvent)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            mockFlushPoliciesFacade.reset()
            storage.rollover()
        }
    }

    @Test
    fun `given default flush policies are enabled, when 30 events are made, then flush call should be triggered`() {
        val storage = mockAnalytics.storage
        val mockEvent: Event = mockk(relaxed = true)
        val jsonString = """{"type":"track","event":"Test Event"}"""
        every { eventQueue.stringifyBaseEvent(mockEvent) } returns jsonString
        // Mock the behavior for StartupFlushPolicy
        every { mockFlushPoliciesFacade.shouldFlush() } returns true

        // Execute messageQueue actions
        eventQueue.start()

        // Make the first event
        eventQueue.put(mockEvent)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            mockFlushPoliciesFacade.reset()
            storage.rollover()
        }

        // Mock the behavior for CountFlushPolicy
        every { mockFlushPoliciesFacade.shouldFlush() } returns false

        repeat(29) {
            eventQueue.put(mockEvent)
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
        eventQueue.put(mockEvent)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 2) {
            mockFlushPoliciesFacade.reset()
            storage.rollover()
        }
    }

    @Test
    fun `given default flush policies are enabled, when events are made, then the flush policies state should be updated`() {
        val storage = mockAnalytics.storage
        val times = 20
        val mockEvent: Event = mockk(relaxed = true)
        val jsonString = """{"type":"track","event":"Test Event"}"""
        every { eventQueue.stringifyBaseEvent(mockEvent) } returns jsonString

        // Execute messageQueue actions
        eventQueue.start()

        repeat(times) {
            eventQueue.put(mockEvent)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = times) {
            storage.write(StorageKeys.EVENT, jsonString)
            mockFlushPoliciesFacade.updateState()
        }
    }

    @Test
    fun `given default flush policies are enabled, when flush is called, then flush policies should be reset`() {
        eventQueue.start()
        eventQueue.flush()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) {
            mockFlushPoliciesFacade.reset()
        }
    }

    @Test
    fun `given default flush policies are enabled, when stop is called, then flush policies should be cancelled`() =
        runTest {
            eventQueue.start()

            eventQueue.stop()
            testDispatcher.scheduler.advanceUntilIdle()

            verify(exactly = 1) {
                mockFlushPoliciesFacade.cancelSchedule()
            }
        }

    @Test
    fun `given no policies are enabled, when explicit flush call is made, then rollover should happen`() {
        val storage = mockAnalytics.storage
        val times = 100
        val mockEvent: Event = mockk(relaxed = true)
        val jsonString = """{"type":"track","event":"Test Event"}"""
        every { eventQueue.stringifyBaseEvent(mockEvent) } returns jsonString
        // Mock the behavior when no flush policies are enabled
        every { mockFlushPoliciesFacade.shouldFlush() } returns false

        // Execute messageQueue actions
        eventQueue.start()
        repeat(times) {
            eventQueue.put(mockEvent)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) {
            storage.rollover()
        }

        eventQueue.flush()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            storage.rollover()
        }
    }
}
