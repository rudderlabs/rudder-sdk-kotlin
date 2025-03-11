package com.rudderstack.sdk.kotlin.android.plugins.sessiontracking

import android.os.SystemClock
import com.rudderstack.sdk.kotlin.android.SessionConfiguration
import com.rudderstack.sdk.kotlin.android.utils.MockMemoryStorage
import com.rudderstack.sdk.kotlin.android.utils.getMonotonicCurrentTime
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.DateTimeUtils
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
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
class SessionTrackingPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var mockAnalytics: Analytics
    private lateinit var mockStorage: Storage
    private lateinit var sessionManager: SessionManager

    private lateinit var sessionTrackingPlugin: SessionTrackingPlugin

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockCurrentMonotonicTime()
        mockSystemCurrentTime()

        sessionTrackingPlugin = spyk(SessionTrackingPlugin())
        mockAnalytics = mockAnalytics(testScope, testDispatcher)
        mockStorage = MockMemoryStorage()
        every { mockAnalytics.storage } returns mockStorage
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
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

        sessionTrackingPlugin.setup(mockAnalytics)
        advanceUntilIdle()

        sessionTrackingPlugin.intercept(firstMessage)
        sessionTrackingPlugin.intercept(secondMessage)

        assertEquals(sessionId.toString(), firstMessage.context[SESSION_ID].toString())
        assertEquals("true", firstMessage.context[SESSION_START].toString())
        assertEquals(sessionId.toString(), secondMessage.context[SESSION_ID].toString())
        assertEquals(null, secondMessage.context[SESSION_START])
    }

    @Test
    fun `given manual session is started from analytics, when intercept called, then correct payload is attached`() =
        runTest {
            val sessionId = 1234567890L
            pluginSetup(automaticSessionTracking = false)

            sessionTrackingPlugin.setup(mockAnalytics)
            advanceUntilIdle()
            sessionManager.startSession(sessionId, true)
            advanceUntilIdle()
            val firstMessage = TrackEvent("test", emptyJsonObject)
            val secondMessage = TrackEvent("test", emptyJsonObject)

            sessionTrackingPlugin.intercept(firstMessage)
            sessionTrackingPlugin.intercept(secondMessage)

            assertEquals(sessionId.toString(), firstMessage.context[SESSION_ID].toString())
            assertEquals("true", firstMessage.context[SESSION_START].toString())
            assertEquals(sessionId.toString(), secondMessage.context[SESSION_ID].toString())
            assertEquals(null, secondMessage.context[SESSION_START])
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

        sessionTrackingPlugin.setup(mockAnalytics)
        advanceUntilIdle()

        sessionTrackingPlugin.intercept(message)
        advanceUntilIdle()

        assertEquals(currentTime - 600_000L, mockStorage.readLong(StorageKeys.LAST_ACTIVITY_TIME, 0L))
    }

    @Test
    fun `given an automatic session, when an event is made, then last activity time is updated`() = runTest(testDispatcher) {
        val sessionId = 1234567890L
        val currentTime = 100000000L
        mockSystemCurrentTime(sessionId * 1000)
        mockCurrentMonotonicTime(currentTime)
        val message = TrackEvent("test", emptyJsonObject)
        pluginSetup(automaticSessionTracking = true)
        mockStorage.write(StorageKeys.SESSION_ID, sessionId)
        mockStorage.write(StorageKeys.IS_SESSION_MANUAL, false)
        mockStorage.write(StorageKeys.LAST_ACTIVITY_TIME, currentTime - 600_000L)

        sessionTrackingPlugin.setup(mockAnalytics)
        advanceUntilIdle()

        sessionTrackingPlugin.intercept(message)
        advanceUntilIdle()

        assertEquals(currentTime, mockStorage.readLong(StorageKeys.LAST_ACTIVITY_TIME, 0L))
    }

    private fun pluginSetup(
        automaticSessionTracking: Boolean = true,
        sessionTimeoutInMillis: Long = 300000L
    ) {
        sessionManager = spyk(
            SessionManager(
                analytics = mockAnalytics, sessionConfiguration = SessionConfiguration(
                    automaticSessionTracking = automaticSessionTracking,
                    sessionTimeoutInMillis = sessionTimeoutInMillis
                )
            )
        )

        every { (mockAnalytics as AndroidAnalytics).sessionManager } returns sessionManager
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
