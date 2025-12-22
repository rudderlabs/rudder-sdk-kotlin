package com.rudderstack.sdk.kotlin.android.plugins.sessiontracking

import android.os.SystemClock
import com.rudderstack.sdk.kotlin.android.DEFAULT_SESSION_TIMEOUT_IN_MILLIS
import com.rudderstack.sdk.kotlin.android.SessionConfiguration
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ProcessLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.MockMemoryStorage
import com.rudderstack.sdk.kotlin.android.utils.addLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.getMonotonicCurrentTime
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.android.utils.removeLifecycleObserver
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.DateTimeUtils
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SessionManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var mockAnalytics: Analytics
    private lateinit var mockStorage: Storage
    private lateinit var sessionConfiguration: SessionConfiguration

    private lateinit var sessionManager: SessionManager

    @BeforeEach
    fun setup() {
        mockCurrentMonotonicTime()
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
    fun `given an automatic session enabled, when app is launched and session is timed out, then new session starts`() =
        runTest(testDispatcher) {
            val automaticSessionTrackingEnabled = true
            val initialSessionId = 1234567890L
            val currentTime = System.currentTimeMillis()
            mockCurrentMonotonicTime(currentTime)
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
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotEquals(initialSessionId, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertEquals(currentTime / 1000, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertFalse(mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
        }

    @Test
    fun `given previous session was manual, when automatic session enabled and app launched, then new automatic session starts`() =
        runTest(testDispatcher) {
            val automaticSessionTrackingEnabled = true
            val initialSessionId = 1234567890L
            mockStorage.write(StorageKeys.SESSION_ID, initialSessionId)
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, true)

            sessionManagerSetup(automaticSessionTracking = automaticSessionTrackingEnabled)
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotEquals(initialSessionId, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertFalse(mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
        }

    @Test
    fun `given no session id stored previously, when automatic session enabled and app launched, then new automatic session is started with correct session id`() =
        runTest(testDispatcher) {
            val automaticSessionTrackingEnabled = true
            val currentTime = System.currentTimeMillis()
            mockSystemCurrentTime(currentTime)

            sessionManagerSetup(automaticSessionTracking = automaticSessionTrackingEnabled)
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
            mockCurrentMonotonicTime(currentTime)
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, false)
            mockStorage.write(StorageKeys.SESSION_ID, previousSessionId)
            mockStorage.write(StorageKeys.LAST_ACTIVITY_TIME, currentTime - 600_000L) // Last event was 10 mins ago

            sessionManagerSetup(
                automaticSessionTracking = automaticSessionTrackingEnabled,
                sessionTimeoutInMillis = 300_000L
            )
            sessionManager.checkAndStartSessionOnForeground() // app is foregrounded
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
    fun `given automatic session enabled and the system is restarted, when app is launched, then new session starts`() =
        runTest(testDispatcher) {
            val automaticSessionTrackingEnabled = true
            val initialSessionId = 1234567890L
            val currentTime = System.currentTimeMillis()
            mockCurrentMonotonicTime(50_000) // the current event is made 50 seconds after the system is restarted
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
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotEquals(initialSessionId, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertEquals(currentTime / 1000, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertFalse(mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
        }

    @Test
    fun `given automatic session enabled and the system is restarted, when app is foregrounded, then new session starts`() =
        runTest(testDispatcher) {
            val automaticSessionTrackingEnabled = true
            val previousSessionId = 1234567890L
            val currentTime = System.currentTimeMillis()
            mockCurrentMonotonicTime(50_000) // the current event is made 50 seconds after the system is restarted
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, false)
            mockStorage.write(StorageKeys.SESSION_ID, previousSessionId)
            mockStorage.write(StorageKeys.LAST_ACTIVITY_TIME, currentTime - 600_000L) // Last event was 10 mins ago

            sessionManagerSetup(
                automaticSessionTracking = automaticSessionTrackingEnabled,
                sessionTimeoutInMillis = 300_000L
            )
            sessionManager.checkAndStartSessionOnForeground() // app is foregrounded
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotEquals(previousSessionId, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertFalse(mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
        }

    private fun sessionManagerSetup(
        automaticSessionTracking: Boolean = true,
        sessionTimeoutInMillis: Long = 300_000L
    ) {
        sessionConfiguration = SessionConfiguration(
            automaticSessionTracking = automaticSessionTracking,
            sessionTimeoutInMillis = sessionTimeoutInMillis
        )

        sessionManager = SessionManager(
            sessionDispatcher = testDispatcher,
            analytics = mockAnalytics,
            sessionConfiguration = sessionConfiguration
        )
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

    private fun mockCurrentMonotonicTime(currentTime: Long = System.currentTimeMillis()) {
        mockkStatic(SystemClock::class)
        every { getMonotonicCurrentTime() } returns currentTime
    }

    private fun mockSystemCurrentTime(currentTime: Long = System.currentTimeMillis()) {
        mockkObject(DateTimeUtils)
        every { DateTimeUtils.getSystemCurrentTime() } returns currentTime
    }
}
