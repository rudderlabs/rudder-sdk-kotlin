package com.rudderstack.sdk.kotlin.android

import android.app.Application
import com.rudderstack.sdk.kotlin.android.logger.AndroidLogger
import com.rudderstack.sdk.kotlin.android.plugins.DeviceInfoPlugin
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleManagementPlugin
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ProcessLifecycleManagementPlugin
import com.rudderstack.sdk.kotlin.android.utils.getMonotonicCurrentTime
import com.rudderstack.sdk.kotlin.core.AnalyticsConfiguration
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger.LogLevel
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.DateTimeUtils
import com.rudderstack.sdk.kotlin.core.provideAnalyticsConfiguration
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val DEFAULT_SESSION_ID: Long = 1234567890L
private const val NEW_SESSION_ID: Long = 9876543210L
private const val TRACK_EVENT_NAME = "Track event 1"
private const val NEW_EVENT_NAME = "New Event Name"

class AnalyticsTest {

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockAnalyticsConfiguration: AnalyticsConfiguration

    @MockK
    private lateinit var mockStorage: Storage

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var configuration: Configuration
    private lateinit var analytics: Analytics

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        mockkConstructor(DeviceInfoPlugin::class)
        every {
            anyConstructed<DeviceInfoPlugin>().getDeviceInfo()
        } returns emptyJsonObject

        // Mock ProcessLifecycleManagementPlugin
        mockkConstructor(ProcessLifecycleManagementPlugin::class)
        every {
            anyConstructed<ProcessLifecycleManagementPlugin>().setup(any())
        } just Runs

        // Mock ActivityLifecycleManagementPlugin
        mockkConstructor(ActivityLifecycleManagementPlugin::class)
        every {
            anyConstructed<ActivityLifecycleManagementPlugin>().setup(any())
        } just Runs

        // Mock Analytics Configuration
        mockkStatic(::provideAnalyticsConfiguration)
        every { provideAnalyticsConfiguration(any()) } returns mockAnalyticsConfiguration
        mockAnalyticsConfiguration.apply {
            every { analyticsScope } returns testScope
            every { analyticsDispatcher } returns testDispatcher
            every { storageDispatcher } returns testDispatcher
            every { networkDispatcher } returns testDispatcher
            every { integrationsDispatcher } returns testDispatcher
            every { storage } returns mockStorage
        }

        // Mock SessionId
        mockkStatic(::getMonotonicCurrentTime)
        every { getMonotonicCurrentTime() } returns DEFAULT_SESSION_ID
        mockkObject(DateTimeUtils)
        every { DateTimeUtils.getSystemCurrentTime() } returns DEFAULT_SESSION_ID.toMilliSeconds()

        // Mock LoggerAnalytics
        mockkObject(LoggerAnalytics)

        configuration = Configuration(
            application = mockApplication,
            writeKey = "<writeKey>",
            dataPlaneUrl = "<data_plane_url>",
        )
        analytics = Analytics(configuration)
    }

    @Test
    fun `given sessionId is of invalid length, when manual session is invoked, then session should not change`() {
        val sessionId = 1234L

        analytics.startSession(sessionId)

        assertEquals(DEFAULT_SESSION_ID, analytics.sessionId)
    }

    @Test
    fun `given manual session is active, when session is ended, then no session info should be present in the events`() {
        val sessionId = 9876543210L
        analytics.startSession(sessionId)

        analytics.endSession()

        assertNull(analytics.sessionId)
    }

    @Test
    fun `given session is active, when sessionID is fetched, then it should match with the expected value`() {
        val sessionId = analytics.sessionId

        assertEquals(DEFAULT_SESSION_ID, sessionId)
    }

    @Test
    fun `given sessionId is of valid length, when manual session is invoked, then a new session should be started`() {
        analytics.startSession(NEW_SESSION_ID)

        assertEquals(NEW_SESSION_ID, analytics.sessionId)
    }

    @Test
    fun `given session is active, when reset is called, then session should refresh`() {
        every { DateTimeUtils.getSystemCurrentTime() } returns NEW_SESSION_ID.toMilliSeconds()

        analytics.reset()

        assertEquals(NEW_SESSION_ID, analytics.sessionId)
    }

    @Test
    fun `when custom plugin is dynamically added, then it should intercept the message and process event`() =
        runTest(testDispatcher) {
            val customPlugin = provideCustomPlugin()

            analytics.add(customPlugin)
            analytics.track(TRACK_EVENT_NAME)
            testDispatcher.scheduler.runCurrent()
            coVerify(exactly = 1) {
                mockStorage.write(StorageKeys.EVENT, withArg<String> { eventString ->
                    assertTrue(eventString.contains(NEW_EVENT_NAME))
                })
            }
        }

    @Test
    fun `when custom plugin is dynamically removed, then it shouldn't intercept the message and process event`() =
        runTest(testDispatcher) {
            val customPlugin = provideCustomPlugin()
            analytics.add(customPlugin)

            analytics.remove(customPlugin)
            analytics.track(TRACK_EVENT_NAME)
            testDispatcher.scheduler.runCurrent()

            coVerify(exactly = 1) {
                mockStorage.write(StorageKeys.EVENT, withArg<String> { eventString ->
                    assertTrue(eventString.contains(TRACK_EVENT_NAME))
                })
            }
        }

    @Test
    fun `when SDK is initialised, then AndroidLogger with default log level should be set`() {
        verify(exactly = 1) {
            LoggerAnalytics.setup(any<AndroidLogger>(), LogLevel.NONE)
        }
    }

    @Test
    fun `given session is active, when shutdown is called, then sessionId should be null`() = runTest(testDispatcher) {
        analytics.shutdown()
        testDispatcher.scheduler.runCurrent()

        assertNull(analytics.sessionId)
    }
}

private fun provideCustomPlugin() = object : Plugin {
    override val pluginType: Plugin.PluginType = Plugin.PluginType.OnProcess
    override lateinit var analytics: com.rudderstack.sdk.kotlin.core.Analytics

    override suspend fun intercept(event: Event): Event? {
        if (event is TrackEvent) {
            event.event = NEW_EVENT_NAME
        }
        return super.intercept(event)
    }
}

private fun Long.toMilliSeconds() = this * 1000L
