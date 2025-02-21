package com.rudderstack.sdk.kotlin.android.plugins.sessiontracking

import android.os.SystemClock
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.DEFAULT_SESSION_TIMEOUT_IN_MILLIS
import com.rudderstack.sdk.kotlin.android.SessionConfiguration
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ProcessLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.MockMemoryStorage
import com.rudderstack.sdk.kotlin.android.utils.addLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.getMonotonicCurrentTime
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.DateTimeUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
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
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics

@OptIn(ExperimentalCoroutinesApi::class)
class SessionTrackingPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var plugin: SessionTrackingPlugin
    private lateinit var mockAnalytics: Analytics

    private lateinit var mockStorage: Storage

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockCurrentMonotonicTime()
        mockSystemCurrentTime()

        plugin = spyk(SessionTrackingPlugin(testDispatcher))
        mockAnalytics = mockAnalytics(testScope, testDispatcher)
        mockStorage = MockMemoryStorage()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `given automatic session tracking enabled, when setup is called, then session tracking observers are added`() {
        pluginSetup(automaticSessionTracking = true)

        plugin.setup(mockAnalytics)

        verify { (mockAnalytics as AndroidAnalytics).addLifecycleObserver(ofType(ProcessLifecycleObserver::class)) }
        verify { (mockAnalytics as AndroidAnalytics).addLifecycleObserver(ofType(ActivityLifecycleObserver::class)) }
    }

    @Test
    fun `given session timeout occured, when app is launched, then new session starts`() = runTest {
        val automaticSessionTrackingEnabled = true
        mockStorage.write(
            StorageKeys.LAST_ACTIVITY_TIME,
            System.currentTimeMillis() - 600_000L
        ) // Last event was 10 mins ago

        pluginSetup(automaticSessionTracking = automaticSessionTrackingEnabled, sessionTimeoutInMillis = 300_000L)

        plugin.setup(mockAnalytics)
        advanceUntilIdle()

        assert(mockStorage.readLong(StorageKeys.SESSION_ID, 0L) != 0L)
        assert(!mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
    }

    @Test
    fun `given previous session was manual and automatic enabled on new launch, when app launched, then new session starts`() =
        runTest {
            val automaticSessionTrackingEnabled = true
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, true)
            pluginSetup(automaticSessionTracking = automaticSessionTrackingEnabled)

            plugin.setup(mockAnalytics)
            advanceUntilIdle()

            assert(mockStorage.readLong(StorageKeys.SESSION_ID, 0L) != 0L)
            assert(!mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
        }

    @Test
    fun `given no session id stored previously and automatic session enabled, when app launched, then new session is started`() =
        runTest {
            val automaticSessionTrackingEnabled = true
            pluginSetup(automaticSessionTracking = automaticSessionTrackingEnabled)

            plugin.setup(mockAnalytics)
            advanceUntilIdle()

            assert(mockStorage.readLong(StorageKeys.SESSION_ID, 0L) != 0L)
            assert(!mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
        }

    @Test
    fun `given automatic session is disabled and previous session was manual, when app launched, then session is not cleared`() =
        runTest {
            val automaticSessionTrackingEnabled = false
            val previousSessionId = 1234567890L
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, true)
            mockStorage.write(StorageKeys.SESSION_ID, previousSessionId)
            pluginSetup(automaticSessionTracking = automaticSessionTrackingEnabled)

            plugin.setup(mockAnalytics)
            advanceUntilIdle()

            assertEquals(previousSessionId, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertEquals(true, mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
        }

    @Test
    fun `given automatic session is disabled and previous session was automatic, when app launched, then session is cleared`() =
        runTest {
            val automaticSessionTrackingEnabled = false
            val previousSessionId = 1234567890L
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, false)
            mockStorage.write(StorageKeys.SESSION_ID, previousSessionId)
            pluginSetup(automaticSessionTracking = automaticSessionTrackingEnabled)

            plugin.setup(mockAnalytics)
            advanceUntilIdle()

            assertEquals(0L, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
            assertEquals(false, mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
            verify { plugin.endSession() }
        }

    @Test
    fun `given automatic session enabled, when intercept called, then correct payload is attached`() = runTest {
        val sessionId = 1234567890L
        val currentTime = 100000000L
        mockSystemCurrentTime(sessionId * 1000)
        mockCurrentMonotonicTime(currentTime)
        val firstMessage = TrackEvent("test", emptyJsonObject)
        val secondMessage = TrackEvent("test", emptyJsonObject)
        pluginSetup(automaticSessionTracking = true)

        plugin.setup(mockAnalytics)
        advanceUntilIdle()

        plugin.intercept(firstMessage)
        plugin.intercept(secondMessage)

        assertEquals(sessionId.toString(), firstMessage.context[SESSION_ID].toString())
        assertEquals("true", firstMessage.context[SESSION_START].toString())
        assertEquals(sessionId.toString(), secondMessage.context[SESSION_ID].toString())
        assertEquals(null, secondMessage.context[SESSION_START])
    }

    @Test
    fun `given manual session is started from analytics, when intercept called, then correct payload is attached`() = runTest {
        val sessionId = 1234567890L
        pluginSetup(automaticSessionTracking = false)

        plugin.setup(mockAnalytics)
        advanceUntilIdle()
        plugin.startSession(sessionId, true)
        advanceUntilIdle()
        val firstMessage = TrackEvent("test", emptyJsonObject)
        val secondMessage = TrackEvent("test", emptyJsonObject)

        plugin.intercept(firstMessage)
        plugin.intercept(secondMessage)

        assertEquals(sessionId.toString(), firstMessage.context[SESSION_ID].toString())
        assertEquals("true", firstMessage.context[SESSION_START].toString())
        assertEquals(sessionId.toString(), secondMessage.context[SESSION_ID].toString())
        assertEquals(null, secondMessage.context[SESSION_START])
    }

    @Test
    fun `given automatic session enabled currently and session is not ended previously and timeout occurs, when checkAndStartSessionOnForeground called, then start new session called`() =
        runTest {
            val automaticSessionTrackingEnabled = true
            val currentTime = System.currentTimeMillis()
            mockStorage.write(StorageKeys.IS_SESSION_MANUAL, false)
            mockStorage.write(StorageKeys.LAST_ACTIVITY_TIME, currentTime - 600_000L) // Last event was 10 mins ago
            mockCurrentMonotonicTime(currentTime)
            pluginSetup(automaticSessionTracking = automaticSessionTrackingEnabled, sessionTimeoutInMillis = 300_000L)

            plugin.setup(mockAnalytics)
            advanceUntilIdle()
            plugin.checkAndStartSessionOnForeground()
            advanceUntilIdle()

            assert(mockStorage.readLong(StorageKeys.SESSION_ID, 0L) != 0L)
            assert(!mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
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
        pluginSetup(automaticSessionTracking = automaticSessionTrackingEnabled, sessionTimeoutInMillis = 300_000L)

        plugin.setup(mockAnalytics)
        advanceUntilIdle()
        plugin.refreshSession()
        advanceUntilIdle()

        assert(mockStorage.readLong(StorageKeys.SESSION_ID, 0L) == currentTime / 1000)
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

        pluginSetup(automaticSessionTracking = automaticSessionTrackingEnabled)
        plugin.setup(mockAnalytics)

        advanceUntilIdle()
        plugin.refreshSession()
        advanceUntilIdle()

        assert(mockStorage.readLong(StorageKeys.SESSION_ID, 0L) == currentTime / 1000)
    }

    @Test
    fun `given automatic session enabled, when endSession called, then all the session variables are cleared`() = runTest {
        mockStorage.write(StorageKeys.SESSION_ID, 1234567890L)
        mockStorage.write(StorageKeys.IS_SESSION_MANUAL, false)
        mockStorage.write(StorageKeys.LAST_ACTIVITY_TIME, System.currentTimeMillis())
        mockStorage.write(StorageKeys.IS_SESSION_START, true)
        pluginSetup(automaticSessionTracking = true)

        plugin.setup(mockAnalytics)
        advanceUntilIdle()
        plugin.endSession()
        advanceUntilIdle()

        assertEquals(0L, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
        assertEquals(false, mockStorage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
        assertEquals(0L, mockStorage.readLong(StorageKeys.LAST_ACTIVITY_TIME, 0L))
        assertEquals(false, mockStorage.readBoolean(StorageKeys.IS_SESSION_START, false))
    }

    @Test
    fun `given manual session is active, when endSession called, then all the session variables are cleared`() = runTest {
        mockStorage.write(StorageKeys.SESSION_ID, 1234567890L)
        mockStorage.write(StorageKeys.IS_SESSION_MANUAL, true)
        mockStorage.write(StorageKeys.LAST_ACTIVITY_TIME, System.currentTimeMillis())
        mockStorage.write(StorageKeys.IS_SESSION_START, true)
        pluginSetup(automaticSessionTracking = false)

        plugin.setup(mockAnalytics)
        advanceUntilIdle()
        plugin.endSession()
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
            pluginSetup(automaticSessionTracking = true, sessionTimeoutInMillis = sessionTimeout)

            plugin.setup(mockAnalytics)

            assertEquals(sessionTimeout, plugin.sessionTimeout)
        }

    @Test
    fun `given manual session is ongoing, when an event is made, then last activity time is not updated`() = runTest {
        val sessionId = 1234567890L
        val currentTime = 100000000L
        mockSystemCurrentTime(sessionId * 1000)
        mockCurrentMonotonicTime(currentTime)
        val message = TrackEvent("test", emptyJsonObject)
        pluginSetup(automaticSessionTracking = false)
        mockStorage.write(StorageKeys.SESSION_ID, sessionId)
        mockStorage.write(StorageKeys.IS_SESSION_MANUAL, true)
        mockStorage.write(StorageKeys.LAST_ACTIVITY_TIME, currentTime - 600_000L)

        plugin.setup(mockAnalytics)
        advanceUntilIdle()

        plugin.intercept(message)
        advanceUntilIdle()

        assertEquals(currentTime - 600_000L, mockStorage.readLong(StorageKeys.LAST_ACTIVITY_TIME, 0L))
    }

    @Test
    fun `given an automatic session, when an event is made, then last activity time is updated`() = runTest {
        val sessionId = 1234567890L
        val currentTime = 100000000L
        mockSystemCurrentTime(sessionId * 1000)
        mockCurrentMonotonicTime(currentTime)
        val message = TrackEvent("test", emptyJsonObject)
        pluginSetup(automaticSessionTracking = true)
        mockStorage.write(StorageKeys.SESSION_ID, sessionId)
        mockStorage.write(StorageKeys.IS_SESSION_MANUAL, false)
        mockStorage.write(StorageKeys.LAST_ACTIVITY_TIME, currentTime - 600_000L)

        plugin.setup(mockAnalytics)
        advanceUntilIdle()

        plugin.intercept(message)
        advanceUntilIdle()

        assertEquals(currentTime, mockStorage.readLong(StorageKeys.LAST_ACTIVITY_TIME, 0L))
    }

    @Test
    fun `given a negative value of session timeout in config, when plugin setup called, then session timeout set as default`() =
        runTest {
            pluginSetup(automaticSessionTracking = true, sessionTimeoutInMillis = -1)

            plugin.setup(mockAnalytics)

            assertEquals(DEFAULT_SESSION_TIMEOUT_IN_MILLIS, plugin.sessionTimeout)
        }

    private fun pluginSetup(
        automaticSessionTracking: Boolean = true,
        sessionTimeoutInMillis: Long = 300000L
    ) {
        val mockConfiguration = mockk<Configuration>(relaxed = true) {
            every { sessionConfiguration } returns SessionConfiguration(automaticSessionTracking, sessionTimeoutInMillis)
            every { mockAnalytics.storage } returns mockStorage
        }

        every { mockAnalytics.configuration } returns mockConfiguration
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
