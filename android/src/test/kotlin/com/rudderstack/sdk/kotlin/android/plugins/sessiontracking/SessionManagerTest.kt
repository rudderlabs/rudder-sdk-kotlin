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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var mockAnalytics: Analytics
    private lateinit var mockStorage: Storage
    private lateinit var sessionConfiguration: SessionConfiguration

    private lateinit var sessionManager: SessionManager

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockCurrentMonotonicTime()
        mockSystemCurrentTime()

        mockAnalytics = mockAnalytics(testScope, testDispatcher)
        mockStorage = MockMemoryStorage()
        every { mockAnalytics.storage } returns mockStorage
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given automatic session tracking enabled, when setup is called, then session tracking observers are added`() {
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
    fun `given session timeout occured, when app is launched, then new session starts`() = runTest {
        val automaticSessionTrackingEnabled = true
        mockStorage.write(
            StorageKeys.LAST_ACTIVITY_TIME,
            System.currentTimeMillis() - 600_000L
        ) // Last event was 10 mins ago

        sessionManagerSetup(automaticSessionTracking = automaticSessionTrackingEnabled, sessionTimeoutInMillis = 300_000L)

        advanceUntilIdle()

        assertNotEquals(0L, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
        assertFalse(mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
    }

    @Test
    fun `given previous session was manual and automatic enabled on new launch, when app launched, then new session starts`() =
        runTest {
            val automaticSessionTrackingEnabled = true
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, true)

            sessionManagerSetup(automaticSessionTracking = automaticSessionTrackingEnabled)
            advanceUntilIdle()

            assertNotEquals(0L, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertFalse(mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
        }

    @Test
    fun `given no session id stored previously and automatic session enabled, when app launched, then new session is started`() =
        runTest {
            val automaticSessionTrackingEnabled = true

            sessionManagerSetup(automaticSessionTracking = automaticSessionTrackingEnabled)
            advanceUntilIdle()

            assertNotEquals(0L, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertFalse(mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
        }

    @Test
    fun `given automatic session is disabled and previous session was manual, when app launched, then session is not cleared`() =
        runTest {
            val automaticSessionTrackingEnabled = false
            val previousSessionId = 1234567890L
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, true)
            mockStorage.write(StorageKeys.SESSION_ID, previousSessionId)

            sessionManagerSetup(automaticSessionTracking = automaticSessionTrackingEnabled)
            advanceUntilIdle()

            assertEquals(previousSessionId, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertEquals(true, mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
        }

    @Test
    fun `given automatic session is disabled and previous session was automatic, when app launched, then session is cleared`() =
        runTest {
            val automaticSessionTrackingEnabled = false
            val previousSessionId = 1234567890L
            val lastActivityTime = System.currentTimeMillis() - 600_000L
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, false)
            mockStorage.write(StorageKeys.SESSION_ID, previousSessionId)
            mockStorage.write(StorageKeys.LAST_ACTIVITY_TIME, lastActivityTime)

            sessionManagerSetup(automaticSessionTracking = automaticSessionTrackingEnabled)
            advanceUntilIdle()

            assertEquals(0L, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertEquals(false, mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
            assertEquals(0L, mockStorage.readLong(StorageKeys.LAST_ACTIVITY_TIME, 0L))
        }

    @Test
    fun `given automatic session enabled currently and session is not ended previously and timeout occurs, when checkAndStartSessionOnForeground called, then start new session called`() =
        runTest {
            val automaticSessionTrackingEnabled = true
            val currentTime = System.currentTimeMillis()
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, false)
            mockStorage.write(StorageKeys.LAST_ACTIVITY_TIME, currentTime - 600_000L) // Last event was 10 mins ago
            mockCurrentMonotonicTime(currentTime)

            sessionManagerSetup(
                automaticSessionTracking = automaticSessionTrackingEnabled,
                sessionTimeoutInMillis = 300_000L
            )
            advanceUntilIdle()
            sessionManager.checkAndStartSessionOnForeground()
            advanceUntilIdle()

            assertNotEquals(0L, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertFalse(mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
        }

    @Test
    fun `given automatic session enabled, when refresh called, then session is refreshed`() = runTest {
        val automaticSessionTrackingEnabled = true
        val previousSessionId = 1234567890L
        val currentTime = System.currentTimeMillis()
        mockStorage.write(StorageKeys.IS_SESSION_MANUAL, false)
        mockStorage.write(StorageKeys.SESSION_ID, previousSessionId)
        mockStorage.write(StorageKeys.LAST_ACTIVITY_TIME, currentTime - 200_000L)
        mockCurrentMonotonicTime(currentTime)
        mockSystemCurrentTime(currentTime)

        sessionManagerSetup(
            automaticSessionTracking = automaticSessionTrackingEnabled,
            sessionTimeoutInMillis = 300_000L
        )
        advanceUntilIdle()
        sessionManager.refreshSession()
        advanceUntilIdle()

        assertEquals(currentTime / 1000, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
    }

    @Test
    fun `given manual session is active, when refresh called, then session is refreshed`() = runTest {
        val automaticSessionTrackingEnabled = false
        val previousSessionId = 1234567890L
        val currentTime = System.currentTimeMillis()
        mockStorage.write(StorageKeys.IS_SESSION_MANUAL, true)
        mockStorage.write(StorageKeys.SESSION_ID, previousSessionId)
        mockkStatic(DateTimeUtils::class)
        every { DateTimeUtils.getSystemCurrentTime() } returns currentTime

        sessionManagerSetup(automaticSessionTracking = automaticSessionTrackingEnabled)

        advanceUntilIdle()
        sessionManager.refreshSession()
        advanceUntilIdle()

        assertEquals(currentTime / 1000, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
    }

    @Test
    fun `given automatic session enabled, when endSession called, then all the session variables are cleared`() =
        runTest {
            mockStorage.write(StorageKeys.SESSION_ID, 1234567890L)
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, false)
            mockStorage.write(StorageKeys.LAST_ACTIVITY_TIME, System.currentTimeMillis())
            mockStorage.write(StorageKeys.IS_SESSION_START, true)

            sessionManagerSetup(automaticSessionTracking = true)
            advanceUntilIdle()
            sessionManager.endSession()
            advanceUntilIdle()

            assertEquals(0L, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertEquals(false, mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
            assertEquals(0L, mockStorage.readLong(StorageKeys.LAST_ACTIVITY_TIME, 0L))
            assertEquals(false, mockStorage.readBoolean(StorageKeys.IS_SESSION_START, false))
        }

    @Test
    fun `given manual session is active, when endSession called, then all the session variables are cleared`() =
        runTest {
            mockStorage.write(StorageKeys.SESSION_ID, 1234567890L)
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, true)
            mockStorage.write(StorageKeys.LAST_ACTIVITY_TIME, System.currentTimeMillis())
            mockStorage.write(StorageKeys.IS_SESSION_START, true)

            sessionManagerSetup(automaticSessionTracking = false)
            advanceUntilIdle()
            sessionManager.endSession()
            advanceUntilIdle()

            assertEquals(0L, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertEquals(false, mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
            assertEquals(0L, mockStorage.readLong(StorageKeys.LAST_ACTIVITY_TIME, 0L))
            assertEquals(false, mockStorage.readBoolean(StorageKeys.IS_SESSION_START, false))
        }

    @Test
    fun `given a value of session timeout in config, when plugin setup called, then session timeout is set correctly`() =
        runTest {
            val sessionTimeout = 600000L

            sessionManagerSetup(
                automaticSessionTracking = true,
                sessionTimeoutInMillis = sessionTimeout
            )

            assertEquals(sessionTimeout, sessionManager.sessionTimeout)
        }

    @Test
    fun `given a negative value of session timeout in config, when plugin setup called, then session timeout set as default`() =
        runTest {
            sessionManagerSetup(
                automaticSessionTracking = true,
                sessionTimeoutInMillis = -1L
            )

            assertEquals(DEFAULT_SESSION_TIMEOUT_IN_MILLIS, sessionManager.sessionTimeout)
        }

    @Test
    fun `given an ongoing session, when endSession is called, then session tracking observers are detached`() = runTest {
        sessionManagerSetup(automaticSessionTracking = true)
        advanceUntilIdle()

        sessionManager.endSession()
        advanceUntilIdle()

        verifyDetachObservers()
    }

    @Test
    fun `when startSession is called for a manual session, then session tracking observers are detached`() = runTest {
        sessionManagerSetup(automaticSessionTracking = true)
        advanceUntilIdle()

        sessionManager.startSession(1234567890L, isSessionManual = true)
        advanceUntilIdle()

        verifyDetachObservers()
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
