package com.rudderstack.sdk.kotlin.android.plugins.sessiontracking

import androidx.lifecycle.LifecycleOwner
import com.rudderstack.sdk.kotlin.android.DEFAULT_SESSION_TIMEOUT_IN_MILLIS
import com.rudderstack.sdk.kotlin.android.SessionConfiguration
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ProcessLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.MockMemoryStorage
import com.rudderstack.sdk.kotlin.android.utils.addLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.android.utils.removeLifecycleObserver
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.DateTimeUtils
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class SessionManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var mockAnalytics: Analytics
    private lateinit var mockStorage: Storage
    private lateinit var sessionConfiguration: SessionConfiguration

    private lateinit var sessionManager: SessionManager

    @BeforeEach
    fun setup() {
        mockSystemCurrentTime()

        mockAnalytics = mockAnalytics(testScope, testDispatcher)
        mockStorage = MockMemoryStorage()
        every { mockAnalytics.storage } returns mockStorage
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `given automatic session tracking enabled, when session manager is initialised, then session tracking observers are added`() {
        sessionManagerSetup(automaticSessionTracking = true)

        verify {
            (mockAnalytics as AndroidAnalytics).addLifecycleObserver(
                ofType(
                    ProcessLifecycleObserver::class
                )
            )
        }
        verify {
            (mockAnalytics as AndroidAnalytics).addLifecycleObserver(
                ofType(
                    ActivityLifecycleObserver::class
                )
            )
        }
    }

    @Test
    fun `given an automatic session enabled, when app is first foregrounded and session is timed out, then new session starts`() =
        runTest(testDispatcher) {
            val automaticSessionTrackingEnabled = true
            val initialSessionId = 1234567890L
            val currentTime = System.currentTimeMillis()
            mockSystemCurrentTime(currentTime)
            mockStorage.write(StorageKeys.SESSION_ID, initialSessionId)
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, false)
            mockStorage.write(
                StorageKeys.LAST_ACTIVITY_TIME,
                currentTime - 600_000L
            ) // Last event was 10 mins ago

            sessionManagerSetup(
                automaticSessionTracking = automaticSessionTrackingEnabled,
                sessionTimeoutInMillis = 300_000L
            )
            sessionManager.maybeStartSessionOnForeground()
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotEquals(initialSessionId, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertEquals(currentTime / 1000, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertFalse(mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
        }

    @Test
    fun `given previous session was manual, when automatic session enabled and app first foregrounded, then new automatic session starts`() =
        runTest(testDispatcher) {
            val automaticSessionTrackingEnabled = true
            val initialSessionId = 1234567890L
            mockStorage.write(StorageKeys.SESSION_ID, initialSessionId)
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, true)

            sessionManagerSetup(automaticSessionTracking = automaticSessionTrackingEnabled)
            sessionManager.maybeStartSessionOnForeground()
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotEquals(initialSessionId, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertFalse(mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
        }

    @Test
    fun `given no session id stored previously, when automatic session enabled and app first foregrounded, then new automatic session is started with correct session id`() =
        runTest(testDispatcher) {
            val automaticSessionTrackingEnabled = true
            val currentTime = System.currentTimeMillis()
            mockSystemCurrentTime(currentTime)

            sessionManagerSetup(automaticSessionTracking = automaticSessionTrackingEnabled)
            sessionManager.maybeStartSessionOnForeground()
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotEquals(0L, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertEquals(currentTime / 1000, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertFalse(mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
        }

    @Test
    fun `given previous session was manual, when automatic session is disabled and app launched, then previous session variables are not cleared`() =
        runTest(testDispatcher) {
            val automaticSessionTrackingEnabled = false
            val previousSessionId = 1234567890L
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, true)
            mockStorage.write(StorageKeys.SESSION_ID, previousSessionId)

            sessionManagerSetup(automaticSessionTracking = automaticSessionTrackingEnabled)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(previousSessionId, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertEquals(true, mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
        }

    @Test
    fun `given previous session was automatic, when automatic session is disabled and app launched, then previous session variables are cleared`() =
        runTest(testDispatcher) {
            val automaticSessionTrackingEnabled = false
            val previousSessionId = 1234567890L
            val lastActivityTime = System.currentTimeMillis() - 600_000L
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, false)
            mockStorage.write(StorageKeys.SESSION_ID, previousSessionId)
            mockStorage.write(StorageKeys.LAST_ACTIVITY_TIME, lastActivityTime)

            sessionManagerSetup(automaticSessionTracking = automaticSessionTrackingEnabled)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(0L, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertEquals(false, mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
            assertEquals(0L, mockStorage.readLong(StorageKeys.LAST_ACTIVITY_TIME, 0L))
        }

    @Test
    fun `given automatic session ongoing previously, when app is foregrounded and session is timed out, then new session starts`() =
        runTest(testDispatcher) {
            val automaticSessionTrackingEnabled = true
            val previousSessionId = 1234567890L
            val currentTime = System.currentTimeMillis()
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, false)
            mockStorage.write(StorageKeys.SESSION_ID, previousSessionId)
            mockStorage.write(StorageKeys.LAST_ACTIVITY_TIME, currentTime - 600_000L) // Last event was 10 mins ago

            sessionManagerSetup(
                automaticSessionTracking = automaticSessionTrackingEnabled,
                sessionTimeoutInMillis = 300_000L
            )
            sessionManager.maybeStartSessionOnForeground() // app is foregrounded
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotEquals(previousSessionId, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertFalse(mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
        }

    @Test
    fun `given automatic session enabled previously, when reset called (which internally calls refreshSession), then session is refreshed`() =
        runTest(testDispatcher) {
            val automaticSessionTrackingEnabled = true
            val previousSessionId = 1234567890L
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, false)
            mockStorage.write(StorageKeys.SESSION_ID, previousSessionId)

            sessionManagerSetup(automaticSessionTracking = automaticSessionTrackingEnabled)
            sessionManager.refreshSession()
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotEquals(previousSessionId, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
        }

    @Test
    fun `given manual session enabled previously, when reset called (which internally calls refreshSession), then session is refreshed`() =
        runTest(testDispatcher) {
            val automaticSessionTrackingEnabled = false
            val previousSessionId = 1234567890L
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, true)
            mockStorage.write(StorageKeys.SESSION_ID, previousSessionId)

            sessionManagerSetup(automaticSessionTracking = automaticSessionTrackingEnabled)
            sessionManager.refreshSession()
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotEquals(previousSessionId, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
        }

    @Test
    fun `given automatic session enabled previously, when session is ended with endSession, then all the session variables are cleared`() =
        runTest(testDispatcher) {
            mockStorage.write(StorageKeys.SESSION_ID, 1234567890L)
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, false)
            mockStorage.write(StorageKeys.LAST_ACTIVITY_TIME, System.currentTimeMillis())
            mockStorage.write(StorageKeys.IS_SESSION_START, true)

            sessionManagerSetup(automaticSessionTracking = true)
            sessionManager.endSession()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(0L, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertEquals(false, mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
            assertEquals(0L, mockStorage.readLong(StorageKeys.LAST_ACTIVITY_TIME, 0L))
            assertEquals(false, mockStorage.readBoolean(StorageKeys.IS_SESSION_START, false))
        }

    @Test
    fun `given manual session enabled previously, when session is ended with endSession, then all the session variables are cleared`() =
        runTest(testDispatcher) {
            mockStorage.write(StorageKeys.SESSION_ID, 1234567890L)
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, true)
            mockStorage.write(StorageKeys.IS_SESSION_START, true)

            sessionManagerSetup(automaticSessionTracking = false)
            sessionManager.endSession()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(0L, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertEquals(false, mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
            assertEquals(false, mockStorage.readBoolean(StorageKeys.IS_SESSION_START, false))
        }

    @Test
    fun `given a value of session timeout in config, when plugin setup called, then session timeout is set correctly`() =
        runTest(testDispatcher) {
            val sessionTimeout = 600000L

            sessionManagerSetup(
                automaticSessionTracking = true,
                sessionTimeoutInMillis = sessionTimeout
            )

            assertEquals(sessionTimeout, sessionManager.sessionTimeout)
        }

    @Test
    fun `given a negative value of session timeout in config, when plugin setup called, then session timeout set as default`() =
        runTest(testDispatcher) {
            sessionManagerSetup(
                automaticSessionTracking = true,
                sessionTimeoutInMillis = -1L
            )

            assertEquals(DEFAULT_SESSION_TIMEOUT_IN_MILLIS, sessionManager.sessionTimeout)
        }

    @Test
    fun `given an ongoing session, when endSession is called, then session tracking observers are detached`() = runTest(testDispatcher) {
        sessionManagerSetup(automaticSessionTracking = true)

        sessionManager.endSession()
        testDispatcher.scheduler.advanceUntilIdle()

        verifyDetachObservers()
    }

    @Test
    fun `when startSession is called for a manual session, then session tracking observers are detached`() = runTest(testDispatcher) {
        sessionManagerSetup(automaticSessionTracking = true)

        sessionManager.startSession(1234567890L, isSessionManual = true)
        testDispatcher.scheduler.advanceUntilIdle()

        verifyDetachObservers()
    }

    @Test
    fun `given a foreground lifecycle event, when isInForeground is read, then it is true`() {
        val observerSlot = captureProcessLifecycleObserver()
        sessionManagerSetup(automaticSessionTracking = true)
        observerSlot.captured.onStart(mockk<LifecycleOwner>())

        assertTrue(sessionManager.isInForeground)
    }

    @Test
    fun `given a background lifecycle event, when isInForeground is read, then it is false`() {
        val observerSlot = captureProcessLifecycleObserver()
        sessionManagerSetup(automaticSessionTracking = true)
        observerSlot.captured.onStart(mockk<LifecycleOwner>())
        observerSlot.captured.onStop(mockk<LifecycleOwner>())

        assertFalse(sessionManager.isInForeground)
    }

    @Test
    fun `given an event from the foreground, when countsAsUserActivity is called, then it counts`() {
        sessionManagerSetup(automaticSessionTracking = true, includeBackgroundEventsInSession = false)

        assertTrue(sessionManager.countsAsUserActivity(wasInForeground = true))
    }

    @Test
    fun `given an event from the background and background events are excluded, when countsAsUserActivity is called, then it does not count`() {
        sessionManagerSetup(automaticSessionTracking = true, includeBackgroundEventsInSession = false)

        assertFalse(sessionManager.countsAsUserActivity(wasInForeground = false))
    }

    @Test
    fun `given an event from the background and background events are included, when countsAsUserActivity is called, then it counts`() {
        sessionManagerSetup(automaticSessionTracking = true, includeBackgroundEventsInSession = true)

        assertTrue(sessionManager.countsAsUserActivity(wasInForeground = false))
    }

    @Test
    fun `given an automatic session in the foreground, when visibleSessionId is read, then it is the session id`() = runTest(testDispatcher) {
        val observerSlot = captureProcessLifecycleObserver()
        sessionManagerSetup(automaticSessionTracking = true)
        observerSlot.captured.onStart(mockk<LifecycleOwner>())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(sessionManager.sessionId, sessionManager.visibleSessionId)
    }

    @Test
    fun `given an automatic session in the background and background events are excluded, when visibleSessionId is read, then it is null`() = runTest(testDispatcher) {
        val observerSlot = captureProcessLifecycleObserver()
        sessionManagerSetup(automaticSessionTracking = true, includeBackgroundEventsInSession = false)
        observerSlot.captured.onStart(mockk<LifecycleOwner>())
        observerSlot.captured.onStop(mockk<LifecycleOwner>())
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(sessionManager.visibleSessionId)
    }

    @Test
    fun `given an automatic session in the background and background events are included, when visibleSessionId is read, then it is the session id`() = runTest(testDispatcher) {
        val observerSlot = captureProcessLifecycleObserver()
        sessionManagerSetup(automaticSessionTracking = true, includeBackgroundEventsInSession = true)
        observerSlot.captured.onStart(mockk<LifecycleOwner>())
        observerSlot.captured.onStop(mockk<LifecycleOwner>())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(sessionManager.sessionId, sessionManager.visibleSessionId)
    }

    @Test
    fun `given a manual session in the background, when visibleSessionId is read, then it is the session id`() = runTest(testDispatcher) {
        sessionManagerSetup(automaticSessionTracking = false, includeBackgroundEventsInSession = false)
        sessionManager.startSession(sessionId = 1234567890L, isSessionManual = true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1234567890L, sessionManager.visibleSessionId)
    }

    @Test
    fun `given automatic session enabled, when the process starts but is never foregrounded, then no session starts`() =
        runTest(testDispatcher) {
            sessionManagerSetup(automaticSessionTracking = true)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(NO_SESSION_ID, sessionManager.sessionId)
            assertEquals(0L, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
        }

    @Test
    fun `given automatic session enabled and a stored session, when the process starts but is never foregrounded, then the stored session is untouched`() =
        runTest(testDispatcher) {
            val previousSessionId = 1234567890L
            mockStorage.write(StorageKeys.SESSION_ID, previousSessionId)
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, false)
            mockStorage.write(StorageKeys.LAST_ACTIVITY_TIME, System.currentTimeMillis() - 600_000L)

            sessionManagerSetup(automaticSessionTracking = true, sessionTimeoutInMillis = 300_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(previousSessionId, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
        }

    @Test
    fun `given background events are included, when the process starts but is never foregrounded, then no session starts`() =
        runTest(testDispatcher) {
            sessionManagerSetup(automaticSessionTracking = true, includeBackgroundEventsInSession = true)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(NO_SESSION_ID, sessionManager.sessionId)
        }

    @Test
    fun `given background events are included and no session, when a background event arrives, then a session starts`() =
        runTest(testDispatcher) {
            sessionManagerSetup(automaticSessionTracking = true, includeBackgroundEventsInSession = true)

            sessionManager.maybeStartSessionOnBackgroundEvent()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(DateTimeUtils.getSystemCurrentTime() / 1000, sessionManager.sessionId)
            assertTrue(sessionManager.isSessionStart)
        }

    @Test
    fun `given background events are included and a live session, when a background event arrives, then the session continues`() =
        runTest(testDispatcher) {
            givenStoredSession(lastActivityTime = DateTimeUtils.getSystemCurrentTime() - 60_000L)
            sessionManagerSetup(automaticSessionTracking = true, sessionTimeoutInMillis = 300_000L, includeBackgroundEventsInSession = true)

            sessionManager.maybeStartSessionOnBackgroundEvent()

            assertEquals(STORED_SESSION_ID, sessionManager.sessionId)
        }

    @Test
    fun `given background events are included and a timed-out session, when a background event arrives, then a new session starts`() =
        runTest(testDispatcher) {
            givenStoredSession(lastActivityTime = DateTimeUtils.getSystemCurrentTime() - 600_000L)
            sessionManagerSetup(automaticSessionTracking = true, sessionTimeoutInMillis = 300_000L, includeBackgroundEventsInSession = true)

            sessionManager.maybeStartSessionOnBackgroundEvent()

            assertEquals(DateTimeUtils.getSystemCurrentTime() / 1000, sessionManager.sessionId)
        }

    @Test
    fun `given background events are included and activity at this exact moment, when a background event arrives, then the session continues`() =
        runTest(testDispatcher) {
            givenStoredSession(lastActivityTime = DateTimeUtils.getSystemCurrentTime())
            sessionManagerSetup(automaticSessionTracking = true, includeBackgroundEventsInSession = true)

            sessionManager.maybeStartSessionOnBackgroundEvent()

            assertEquals(STORED_SESSION_ID, sessionManager.sessionId)
        }

    @Test
    fun `given background events are included and a stored manual session, when a background event arrives, then an automatic session replaces it`() =
        runTest(testDispatcher) {
            givenStoredSession(lastActivityTime = DateTimeUtils.getSystemCurrentTime(), isSessionManual = true)
            sessionManagerSetup(automaticSessionTracking = true, includeBackgroundEventsInSession = true)

            sessionManager.maybeStartSessionOnBackgroundEvent()

            assertEquals(DateTimeUtils.getSystemCurrentTime() / 1000, sessionManager.sessionId)
            assertFalse(sessionManager.isSessionManual)
        }

    @Test
    fun `given background events are excluded, when a background event arrives, then no session starts`() =
        runTest(testDispatcher) {
            sessionManagerSetup(automaticSessionTracking = true, includeBackgroundEventsInSession = false)

            sessionManager.maybeStartSessionOnBackgroundEvent()

            assertEquals(NO_SESSION_ID, sessionManager.sessionId)
        }

    @Test
    fun `given background events are included and the app ended the session, when a background event arrives, then no session starts`() =
        runTest(testDispatcher) {
            sessionManagerSetup(automaticSessionTracking = true, includeBackgroundEventsInSession = true)
            sessionManager.endSession()

            sessionManager.maybeStartSessionOnBackgroundEvent()

            assertEquals(NO_SESSION_ID, sessionManager.sessionId)
        }

    @Test
    fun `given the app ended the session, when a foreground callback already in flight runs, then no session starts`() =
        runTest(testDispatcher) {
            sessionManagerSetup(automaticSessionTracking = true)
            sessionManager.endSession()

            sessionManager.maybeStartSessionOnForeground()

            assertEquals(NO_SESSION_ID, sessionManager.sessionId)
        }

    @Test
    fun `given the app started a manual session, when a foreground callback already in flight runs, then the manual session remains`() =
        runTest(testDispatcher) {
            sessionManagerSetup(automaticSessionTracking = true)
            sessionManager.startSession(sessionId = STORED_SESSION_ID, isSessionManual = true)

            sessionManager.maybeStartSessionOnForeground()

            assertEquals(STORED_SESSION_ID, sessionManager.sessionId)
            assertTrue(sessionManager.isSessionManual)
        }

    @Test
    fun `given a background event started a session, when the app comes to the foreground at once, then the same session continues`() =
        runTest(testDispatcher) {
            givenStoredSession(lastActivityTime = DateTimeUtils.getSystemCurrentTime() - 600_000L)
            sessionManagerSetup(automaticSessionTracking = true, sessionTimeoutInMillis = 300_000L, includeBackgroundEventsInSession = true)
            sessionManager.maybeStartSessionOnBackgroundEvent()
            val startedSessionId = sessionManager.sessionId
            sessionManager.updateIsSessionStartIfChanged(false)

            sessionManager.maybeStartSessionOnForeground()

            assertEquals(startedSessionId, sessionManager.sessionId)
            assertFalse(sessionManager.isSessionStart)
        }

    @Test
    fun `given activity in the same millisecond, when the app is first foregrounded, then the stored session continues`() =
        runTest(testDispatcher) {
            givenStoredSession(lastActivityTime = DateTimeUtils.getSystemCurrentTime())
            sessionManagerSetup(automaticSessionTracking = true)

            sessionManager.maybeStartSessionOnForeground()

            assertEquals(STORED_SESSION_ID, sessionManager.sessionId)
        }

    @Test
    fun `given automatic tracking and a manual session left by an earlier process, when the SDK starts, then no session remains`() =
        runTest(testDispatcher) {
            givenStoredSession(lastActivityTime = DateTimeUtils.getSystemCurrentTime(), isSessionManual = true)

            sessionManagerSetup(automaticSessionTracking = true)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(NO_SESSION_ID, sessionManager.sessionId)
            assertNull(sessionManager.visibleSessionId)
            assertEquals(0L, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
        }

    @Test
    fun `given background events are included, when the app ends the session while a background event starts one, then no session remains`() {
        sessionManagerSetup(automaticSessionTracking = true, includeBackgroundEventsInSession = true)
        val manager = spyk(sessionManager)
        val racingCallDone = runOnAnotherThreadWhileStarting(manager) { manager.endSession() }

        manager.maybeStartSessionOnBackgroundEvent()
        racingCallDone.await(2, TimeUnit.SECONDS)

        assertEquals(NO_SESSION_ID, manager.sessionId)
    }

    @Test
    fun `given background events are included, when the app starts a manual session while a background event starts one, then the manual session remains`() {
        sessionManagerSetup(automaticSessionTracking = true, includeBackgroundEventsInSession = true)
        val manager = spyk(sessionManager)
        val racingCallDone = runOnAnotherThreadWhileStarting(manager) {
            manager.startSession(sessionId = STORED_SESSION_ID, isSessionManual = true)
        }

        manager.maybeStartSessionOnBackgroundEvent()
        racingCallDone.await(2, TimeUnit.SECONDS)

        assertEquals(STORED_SESSION_ID, manager.sessionId)
        assertTrue(manager.isSessionManual)
    }

    @Test
    fun `given background events are included and a timed-out session, when visibleSessionId is read in the background, then it is null`() =
        runTest(testDispatcher) {
            givenStoredSession(lastActivityTime = DateTimeUtils.getSystemCurrentTime() - 600_000L)
            sessionManagerSetup(automaticSessionTracking = true, sessionTimeoutInMillis = 300_000L, includeBackgroundEventsInSession = true)

            assertNull(sessionManager.visibleSessionId)
        }

    @Test
    fun `given automatic session enabled and the app was never foregrounded, when startSession is called, then a manual session still starts`() =
        runTest(testDispatcher) {
            val manualSessionId = 1234567890L
            sessionManagerSetup(automaticSessionTracking = true)

            sessionManager.startSession(manualSessionId, isSessionManual = true)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(manualSessionId, sessionManager.sessionId)
            assertEquals(manualSessionId, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertTrue(mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
        }

    @Test
    fun `given an automatic session started on the first foreground, when the app is foregrounded again within the timeout, then the session id is unchanged`() =
        runTest(testDispatcher) {
            sessionManagerSetup(automaticSessionTracking = true, sessionTimeoutInMillis = 300_000L)

            sessionManager.maybeStartSessionOnForeground()
            testDispatcher.scheduler.advanceUntilIdle()
            val firstSessionId = sessionManager.sessionId
            sessionManager.updateLastActivityTime()
            sessionManager.maybeStartSessionOnForeground()
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotEquals(NO_SESSION_ID, firstSessionId)
            assertEquals(firstSessionId, sessionManager.sessionId)
        }

    private fun sessionManagerSetup(
        automaticSessionTracking: Boolean = true,
        sessionTimeoutInMillis: Long = 300_000L,
        includeBackgroundEventsInSession: Boolean = false,
    ) {
        sessionConfiguration = SessionConfiguration(
            automaticSessionTracking = automaticSessionTracking,
            sessionTimeoutInMillis = sessionTimeoutInMillis,
            includeBackgroundEventsInSession = includeBackgroundEventsInSession
        )

        sessionManager = SessionManager(
            sessionDispatcher = testDispatcher,
            analytics = mockAnalytics,
            sessionConfiguration = sessionConfiguration
        )
    }

    private suspend fun givenStoredSession(lastActivityTime: Long, isSessionManual: Boolean = false) {
        mockStorage.write(StorageKeys.SESSION_ID, STORED_SESSION_ID)
        mockStorage.write(StorageKeys.IS_SESSION_MANUAL, isSessionManual)
        mockStorage.write(StorageKeys.LAST_ACTIVITY_TIME, lastActivityTime)
    }

    // Pauses a session start where it picks the id, and runs `racingCall` on another thread meanwhile.
    private fun runOnAnotherThreadWhileStarting(manager: SessionManager, racingCall: () -> Unit): CountDownLatch {
        val racingCallDone = CountDownLatch(1)
        every { manager.generateSessionId() } answers {
            thread { racingCall(); racingCallDone.countDown() }
            racingCallDone.await(RACE_WINDOW_MS, TimeUnit.MILLISECONDS)
            callOriginal()
        }
        return racingCallDone
    }

    private fun captureProcessLifecycleObserver(): CapturingSlot<ProcessLifecycleObserver> {
        // SessionManager owns the observer privately; capture it via MockK so we can drive lifecycle callbacks.
        val observerSlot = slot<ProcessLifecycleObserver>()
        every { (mockAnalytics as AndroidAnalytics).addLifecycleObserver(capture(observerSlot)) } returns Unit
        return observerSlot
    }

    private fun verifyDetachObservers() {
        verify {
            (mockAnalytics as AndroidAnalytics).removeLifecycleObserver(
                ofType(
                    ProcessLifecycleObserver::class
                )
            )
        }
        verify {
            (mockAnalytics as AndroidAnalytics).removeLifecycleObserver(
                ofType(
                    ActivityLifecycleObserver::class
                )
            )
        }
    }

    private fun mockSystemCurrentTime(currentTime: Long = System.currentTimeMillis()) {
        mockkObject(DateTimeUtils)
        every { DateTimeUtils.getSystemCurrentTime() } returns currentTime
    }
}

private const val STORED_SESSION_ID = 1234567890L
private const val RACE_WINDOW_MS = 200L
