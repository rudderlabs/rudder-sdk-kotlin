package com.rudderstack.android.sdk.plugins.sessiontracking

import com.rudderstack.android.sdk.Configuration
import com.rudderstack.android.sdk.SessionConfiguration
import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ProcessLifecycleObserver
import com.rudderstack.android.sdk.utils.MockMemoryStorage
import com.rudderstack.android.sdk.utils.addLifecycleObserver
import com.rudderstack.android.sdk.utils.mockAnalytics
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.TrackEvent
import com.rudderstack.kotlin.sdk.internals.models.emptyJsonObject
import com.rudderstack.kotlin.sdk.internals.storage.Storage
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys
import io.mockk.every
import io.mockk.mockk
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
import com.rudderstack.android.sdk.Analytics as AndroidAnalytics

@OptIn(ExperimentalCoroutinesApi::class)
class SessionTrackingPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var plugin: SessionTrackingPlugin
    private lateinit var mockAnalytics: Analytics

    private lateinit var mockStorage: Storage

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        plugin = spyk(SessionTrackingPlugin(testDispatcher))
        mockAnalytics = mockAnalytics(testScope, testDispatcher)
        mockStorage = MockMemoryStorage()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `given automatic session tracking enabled, when setup is called, then session tracking observers are added`() {
        pluginSetup(automaticSessionTracking = true)

        verify { (mockAnalytics as AndroidAnalytics).addLifecycleObserver(ofType(ProcessLifecycleObserver::class)) }
        verify { (mockAnalytics as AndroidAnalytics).addLifecycleObserver(ofType(ActivityLifecycleObserver::class)) }
    }

    @Test
    fun `given session timeout, when app is launched, then new session starts`() = runTest {
        // given
        val automaticSessionTrackingEnabled = true
        mockStorage.write(StorageKeys.LAST_EVENT_TIME, System.currentTimeMillis() - 600_000L) // Last event was 10 mins ago

        // when
        pluginSetup(automaticSessionTracking = automaticSessionTrackingEnabled, sessionTimeoutInMillis = 300_000L)
        advanceUntilIdle()

        // then
        assert(mockStorage.readLong(StorageKeys.SESSION_ID, 0L) != 0L)
        assert(!mockStorage.readBoolean(StorageKeys.IS_MANUAL_SESSION, false))
    }

    @Test
    fun `given previous session was manual and automatic enabled on new launch, when app launched, then new session starts`() =
        runTest {
            // given
            val automaticSessionTrackingEnabled = true
            mockStorage.write(StorageKeys.IS_MANUAL_SESSION, true)

            // when
            pluginSetup(automaticSessionTracking = automaticSessionTrackingEnabled)
            advanceUntilIdle()

            // then
            assert(mockStorage.readLong(StorageKeys.SESSION_ID, 0L) != 0L)
            assert(!mockStorage.readBoolean(StorageKeys.IS_MANUAL_SESSION, false))
        }

    @Test
    fun `given no session id stored previously and automatic session enabled, when app launched, then new session is started`() = runTest {
        // given
        val automaticSessionTrackingEnabled = true

        // when
        pluginSetup(automaticSessionTracking = automaticSessionTrackingEnabled)
        advanceUntilIdle()

        // then
        assert(mockStorage.readLong(StorageKeys.SESSION_ID, 0L) != 0L)
        assert(!mockStorage.readBoolean(StorageKeys.IS_MANUAL_SESSION, false))
    }

    @Test
    fun `given automatic session is disabled and previous session was manual, when app launched, then session is not cleared`() = runTest {
        // given
        val automaticSessionTrackingEnabled = false
        val previousSessionId = 1234567890L
        mockStorage.write(StorageKeys.IS_MANUAL_SESSION, true)
        mockStorage.write(StorageKeys.SESSION_ID, previousSessionId)

        // when
        pluginSetup(automaticSessionTracking = automaticSessionTrackingEnabled)
        advanceUntilIdle()

        // then
        assertEquals(previousSessionId, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
        assertEquals(true, mockStorage.readBoolean(StorageKeys.IS_MANUAL_SESSION, false))
    }

    @Test
    fun `given automatic session is disabled and previous session was not manual, when app launched, then session is cleared`() = runTest {
        // given
        val automaticSessionTrackingEnabled = false
        val previousSessionId = 1234567890L
        mockStorage.write(StorageKeys.IS_MANUAL_SESSION, false)
        mockStorage.write(StorageKeys.SESSION_ID, previousSessionId)

        // when
        pluginSetup(automaticSessionTracking = automaticSessionTrackingEnabled)
        advanceUntilIdle()

        // then
        assertEquals(0L, mockStorage.readLong(StorageKeys.SESSION_ID, 0L))
        assertEquals(false, mockStorage.readBoolean(StorageKeys.IS_MANUAL_SESSION, false))
    }

    @Test
    fun `automatic session enabled, when execute called, then correct payload is attached`() = runTest {
        // given
        val sessionId = 1234567890L
        every { plugin.getCurrentTime() } returns sessionId * 1000
        val firstMessage = TrackEvent("test", emptyJsonObject)
        val secondMessage = TrackEvent("test", emptyJsonObject)

        pluginSetup(automaticSessionTracking = true)
        advanceUntilIdle()

        // when
        plugin.execute(firstMessage)
        plugin.execute(secondMessage)

        // then
        assertEquals(sessionId.toString(), firstMessage.context[SESSION_ID].toString())
        assertEquals("true", firstMessage.context[SESSION_START].toString())
        assertEquals(sessionId.toString(), secondMessage.context[SESSION_ID].toString())
        assertEquals(null, secondMessage.context[SESSION_START])
    }

    @Test
    fun `given manual session is started from analytics, when execute called, then correct payload is attached`() = runTest {
        // given
        val sessionId = 1234567890L

        pluginSetup(automaticSessionTracking = false)
        advanceUntilIdle()
        plugin.startSession(sessionId, true)
        advanceUntilIdle()
        val firstMessage = TrackEvent("test", emptyJsonObject)
        val secondMessage = TrackEvent("test", emptyJsonObject)

        // when
        plugin.execute(firstMessage)
        plugin.execute(secondMessage)

        // then
        assertEquals(sessionId.toString(), firstMessage.context[SESSION_ID].toString())
        assertEquals("true", firstMessage.context[SESSION_START].toString())
        assertEquals(sessionId.toString(), secondMessage.context[SESSION_ID].toString())
        assertEquals(null, secondMessage.context[SESSION_START])
    }

    @Test
    fun `given automatic session and session is not ended previously and timeout, when checkAndStartSessionOnForeground called, then start new session called`() = runTest {
        // given
        val automaticSessionTrackingEnabled = true
        mockStorage.write(StorageKeys.IS_MANUAL_SESSION, false)
        mockStorage.write(StorageKeys.LAST_EVENT_TIME, System.currentTimeMillis() - 600_000L) // Last event was 10 mins ago

        // when
        pluginSetup(automaticSessionTracking = automaticSessionTrackingEnabled, sessionTimeoutInMillis = 300_000L)
        advanceUntilIdle()
        plugin.checkAndStartSessionOnForeground()
        advanceUntilIdle()

        // then
        assert(mockStorage.readLong(StorageKeys.SESSION_ID, 0L) != 0L)
        assert(!mockStorage.readBoolean(StorageKeys.IS_MANUAL_SESSION, false))
    }

    private fun pluginSetup(
        automaticSessionTracking: Boolean = true,
        sessionTimeoutInMillis: Long = 300000L
    ) {
        val mockConfiguration = mockk<Configuration>(relaxed = true) {
            every { sessionConfiguration } returns SessionConfiguration(automaticSessionTracking, sessionTimeoutInMillis)
            every { storage } returns mockStorage
        }

        every { mockAnalytics.configuration } returns mockConfiguration

        plugin.setup(mockAnalytics)
    }
}
