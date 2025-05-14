package com.rudderstack.sdk.kotlin.core.internals.queue

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.KotlinLogger
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.provider.provideEvent
import com.rudderstack.sdk.kotlin.core.internals.policies.FlushPoliciesFacade
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToString
import com.rudderstack.sdk.kotlin.core.mockAnalytics
import com.rudderstack.sdk.kotlin.core.setupLogger
import io.mockk.MockKAnnotations
import io.mockk.clearMocks
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventQueueTest {

    @MockK
    private lateinit var mockStorage: Storage

    @MockK
    private lateinit var mockKotlinLogger: KotlinLogger

    @MockK
    private lateinit var mockFlushPoliciesFacade: FlushPoliciesFacade

    @MockK
    private lateinit var mockEventUpload: EventUpload

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
                flushPoliciesFacade = mockFlushPoliciesFacade,
                eventUpload = mockEventUpload,
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
            mockEventUpload.flush()
        }
    }

    @Test
    fun `given default flush policies are enabled, when 30 events are made, then flush call should be triggered`() {
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
            mockEventUpload.flush()
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
            mockEventUpload.flush()
        }

        // Mock the behavior for CountFlushPolicy
        every { mockFlushPoliciesFacade.shouldFlush() } returns true

        // Make the 30th event
        eventQueue.put(mockEvent)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 2) {
            mockFlushPoliciesFacade.reset()
            mockEventUpload.flush()
        }
    }

    @Test
    fun `given default flush policies are enabled but source is disabled, when events are made, the flush call is never triggered`() {
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
            mockEventUpload.flush()
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
            mockEventUpload.flush()
        }

        // Mock the behavior for CountFlushPolicy
        every { mockFlushPoliciesFacade.shouldFlush() } returns true

        // Make the 30th event
        eventQueue.put(mockEvent)
        testDispatcher.scheduler.advanceUntilIdle()

        // no flush call is triggered
        coVerify(exactly = 0) {
            mockFlushPoliciesFacade.reset()
            mockEventUpload.flush()
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
    fun `given no policies are enabled, when explicit flush call is made, then flush call should happen`() {
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
            mockEventUpload.flush()
        }

        eventQueue.flush()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            mockEventUpload.flush()
        }
    }

    @Test
    fun `when event queue is started, then event upload is also started`() {
        eventQueue.start()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockEventUpload.start() }
    }

    @Test
    fun `given event queue is started, when event queue is stopped, then event upload is also stopped`() {
        eventQueue.start()

        eventQueue.stop()

        verify { mockEventUpload.cancel() }
    }
}
