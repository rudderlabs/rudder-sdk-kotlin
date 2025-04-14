package com.rudderstack.sdk.kotlin.core.internals.queue

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.KotlinLogger
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.provider.provideEvent
import com.rudderstack.sdk.kotlin.core.internals.network.NetworkErrorStatus
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClient
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.policies.FlushPoliciesFacade
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToBase64
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToString
import com.rudderstack.sdk.kotlin.core.internals.utils.generateUUID
import com.rudderstack.sdk.kotlin.core.mockAnalytics
import com.rudderstack.sdk.kotlin.core.setupLogger
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
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

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)

        setupLogger(mockKotlinLogger)

        every { mockAnalytics.storage } returns mockStorage

        coEvery { mockStorage.close() } just runs
        coEvery { mockStorage.write(StorageKeys.EVENT, any<String>()) } just runs
        every { mockAnalytics.sourceConfigState } returns State(SourceConfig.initialState())
        every { mockAnalytics.sourceConfigState } returns State(
            SourceConfig(
                source = SourceConfig.initialState().source.copy(
                    isSourceEnabled = true
                )
            )
        )

        eventQueue = spyk(
            EventQueue(
                analytics = mockAnalytics,
                httpClientFactory = mockHttpClient,
                flushPoliciesFacade = mockFlushPoliciesFacade,
            )
        )
    }

    @AfterEach
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
            every { eventQueue.doesFileExist(any()) } returns true

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
    fun `given batches of events with different anonymousIds, when they are uploaded, then header is updated for each batch with different anonymousId`() {
        val storage = mockAnalytics.storage

        val filePaths = listOf(
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
        )
        val fileUrlList = filePaths.joinToString(",")

        val batchPayload1 = "test content 1"
        val batchPayload2 = "test content 2"
        val anonymousId1 = "anonymousId1"
        val anonymousId2 = "anonymousId2"

        coEvery {
            storage.readString(StorageKeys.EVENT, String.empty())
        } returns fileUrlList
        every { eventQueue.doesFileExist(any()) } returns true
        every { eventQueue.readFileAsString(filePaths[0]) } returns batchPayload1
        every { eventQueue.readFileAsString(filePaths[1]) } returns batchPayload2

        every { eventQueue.getAnonymousIdFromBatch(batchPayload1) } returns anonymousId1
        every { eventQueue.getAnonymousIdFromBatch(batchPayload2) } returns anonymousId2

        // Mock the behavior for HttpClient
        every { mockHttpClient.sendData(batchPayload1) } returns Result.Success("Ok")
        every { mockHttpClient.sendData(batchPayload2) } returns Result.Success("Ok")

        // Execute messageQueue actions
        eventQueue.start()
        eventQueue.flush()

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            mockHttpClient.updateAnonymousIdHeaderString(anonymousId1.encodeToBase64())
            mockHttpClient.updateAnonymousIdHeaderString(anonymousId2.encodeToBase64())
        }
    }

    @ParameterizedTest
    @MethodSource("batchAnonymousIdTestProvider")
    fun `given a batch with some anonymousId, when it is uploaded, then header is updated with correct anonymousId`(
        batchPayload: String,
        anonymousIdFromBatch: String
    ) = runTest(testDispatcher) {
        val storage = mockAnalytics.storage

        val filePaths = listOf(
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0"
        )
        val fileUrlList = filePaths.joinToString(",")

        coEvery {
            storage.readString(StorageKeys.EVENT, String.empty())
        } returns fileUrlList
        every { eventQueue.doesFileExist(any()) } returns true
        every { eventQueue.readFileAsString(filePaths[0]) } returns batchPayload

        // Mock the behavior for HttpClient
        every { mockHttpClient.sendData(batchPayload) } returns Result.Success("Ok")

        val randomUUID = "some_random_id"
        mockkStatic(::generateUUID)
        every { generateUUID() } returns randomUUID

        // Execute messageQueue actions
        eventQueue.start()
        eventQueue.flush()
        testDispatcher.scheduler.advanceUntilIdle()

        val encodedAnonymousId = anonymousIdFromBatch.encodeToBase64()
        coVerify(atLeast = 1) {
            mockHttpClient.updateAnonymousIdHeaderString(encodedAnonymousId)
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
        every { eventQueue.doesFileExist(any()) } returns true

        val batchPayload = "test content"

        // Mock messageQueue file reading
        filePaths.forEach { path ->
            every { eventQueue.readFileAsString(path) } returns batchPayload
        }

        // Mock the behavior for HttpClient
        every { mockHttpClient.sendData(batchPayload) } returns Result.Failure(
            NetworkErrorStatus.GENERAL_ERROR,
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
        every { eventQueue.doesFileExist(any()) } returns true

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
        every { eventQueue.doesFileExist(any()) } returns true

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
    fun `given a stream of events containing different anonymous ids, when these events are made, then storage rolled over for each different anonymousId`() =
        runTest {
            val storage = mockAnalytics.storage
            val mockEvent1: Event = mockk(relaxed = true)
            val mockEvent2: Event = mockk(relaxed = true)
            every { mockEvent1.anonymousId } returns "anonymousId1"
            every { mockEvent2.anonymousId } returns "anonymousId1"

            val mockEvent3: Event = mockk(relaxed = true)
            val mockEvent4: Event = mockk(relaxed = true)
            every { mockEvent3.anonymousId } returns "anonymousId2"
            every { mockEvent4.anonymousId } returns "anonymousId2"

            eventQueue.start()

            eventQueue.put(mockEvent1)
            eventQueue.put(mockEvent2)
            eventQueue.put(mockEvent3)
            eventQueue.put(mockEvent4)

            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 2) {
                storage.rollover()
            }
        }

    @Test
    fun `given a stream of events containing different anonymous ids, when these events are made, then the last_event_anonymous_id is updated`() =
        runTest {
            val storage = mockAnalytics.storage
            val mockEvent1: Event = mockk(relaxed = true)
            val mockEvent2: Event = mockk(relaxed = true)
            every { mockEvent1.anonymousId } returns "anonymousId1"
            every { mockEvent2.anonymousId } returns "anonymousId1"

            val mockEvent3: Event = mockk(relaxed = true)
            val mockEvent4: Event = mockk(relaxed = true)
            every { mockEvent3.anonymousId } returns "anonymousId2"
            every { mockEvent4.anonymousId } returns "anonymousId2"

            eventQueue.start()

            eventQueue.put(mockEvent1)
            eventQueue.put(mockEvent2)
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify(exactly = 1) {
                storage.write(StorageKeys.LAST_EVENT_ANONYMOUS_ID, "anonymousId1")
            }

            eventQueue.put(mockEvent3)
            eventQueue.put(mockEvent4)

            testDispatcher.scheduler.advanceUntilIdle()
            coVerify(exactly = 1) {
                storage.write(StorageKeys.LAST_EVENT_ANONYMOUS_ID, "anonymousId2")
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
        every { mockAnalytics.sourceConfigState } returns State(
            SourceConfig(
                source = SourceConfig.initialState().source.copy(
                    isSourceEnabled = true
                )
            )
        )

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
        every { mockAnalytics.sourceConfigState } returns State(
            SourceConfig(
                source = SourceConfig.initialState().source.copy(
                    isSourceEnabled = true
                )
            )
        )

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
    fun `given default flush policies are enabled but source is disabled, when events are made, the flush call is never triggered`() {
        val storage = mockAnalytics.storage
        val mockEvent: Event = mockk(relaxed = true)
        val jsonString = """{"type":"track","event":"Test Event"}"""
        every { eventQueue.stringifyBaseEvent(mockEvent) } returns jsonString
        // Mock the behavior for StartupFlushPolicy
        every { mockFlushPoliciesFacade.shouldFlush() } returns true
        every { mockAnalytics.sourceConfigState } returns State(
            SourceConfig(
                source = SourceConfig.initialState().source.copy(isSourceEnabled = false)
            )
        )
        every { mockAnalytics.sourceConfigState } returns State(
            SourceConfig(
                source = SourceConfig.initialState().source.copy(
                    isSourceEnabled = false
                )
            )
        )

        // Execute messageQueue actions
        eventQueue.start()

        // Make the first event
        eventQueue.put(mockEvent)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) {
            mockFlushPoliciesFacade.reset()
            storage.rollover()
        }

        // Mock the behavior for CountFlushPolicy
        every { mockFlushPoliciesFacade.shouldFlush() } returns false

        repeat(29) {
            eventQueue.put(mockEvent)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // No flush should be triggered
        coVerify(exactly = 0) {
            mockFlushPoliciesFacade.reset()
            storage.rollover()
        }

        // Mock the behavior for CountFlushPolicy
        every { mockFlushPoliciesFacade.shouldFlush() } returns true

        // Make the 30th event
        eventQueue.put(mockEvent)
        testDispatcher.scheduler.advanceUntilIdle()

        // no flush call is triggered
        coVerify(exactly = 0) {
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

    companion object {

        @JvmStatic
        fun batchAnonymousIdTestProvider() = listOf(
            Arguments.of(
                """{"userId": "12345", "anonymousId": "abc-123", "event": "test"}""",
                "abc-123"
            ),
            Arguments.of(
                """{"userId": "12345", "event": "test", "anonymousId":"xyz-456"}""",
                "xyz-456"
            ),
            Arguments.of(
                """{"anonymousId": "lmn-789"}""",
                "lmn-789"
            ),
            Arguments.of(
                """{"userId": "12345", "event": "test"}""",
                "some_random_id"
            )
        )
    }
}
