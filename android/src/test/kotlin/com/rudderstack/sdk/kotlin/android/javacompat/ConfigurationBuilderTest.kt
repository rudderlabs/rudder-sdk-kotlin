package com.rudderstack.sdk.kotlin.android.javacompat

import android.app.Application
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.utils.provideSessionConfiguration
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.policies.FlushPolicy
import com.rudderstack.sdk.kotlin.core.provideDefaultFlushPolicies
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val TEST_WRITE_KEY = "test-write-key"
private const val TEST_DATA_PLANE_URL = "https://test-data-plane.com"
private const val TEST_CONTROL_PLANE_URL = "https://test-control-plane.com"
private const val CUSTOM_SESSION_TIMEOUT = 30000L

class ConfigurationBuilderTest {

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockPolicies: List<FlushPolicy>

    private lateinit var configurationBuilder: ConfigurationBuilder

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        mockkStatic("com.rudderstack.sdk.kotlin.core.ConfigurationKt")
        every { provideDefaultFlushPolicies() } returns mockPolicies

        mockkObject(LoggerAnalytics)
        every { LoggerAnalytics.logLevel } returns Logger.DEFAULT_LOG_LEVEL

        configurationBuilder = ConfigurationBuilder(
            mockApplication,
            TEST_WRITE_KEY, TEST_DATA_PLANE_URL
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `when Configuration object is created with only default values, then it should have default values`() {
        val configuration = configurationBuilder.build()

        val expected =
            Configuration(application = mockApplication, writeKey = TEST_WRITE_KEY, dataPlaneUrl = TEST_DATA_PLANE_URL, logger = configuration.logger)
        assertEquals(expected, configuration)
    }

    @Test
    fun `when setTrackApplicationLifecycleEvents is set to false, then trackApplicationLifecycleEvents should be false`() {
        val configuration = configurationBuilder.setTrackApplicationLifecycleEvents(false).build()

        assertFalse(configuration.trackApplicationLifecycleEvents)
    }

    @Test
    fun `when setTrackDeepLinks is set to false, then trackDeepLinks should be false`() {
        val configuration = configurationBuilder.setTrackDeepLinks(false).build()

        assertFalse(configuration.trackDeepLinks)
    }

    @Test
    fun `when setTrackActivities is set to true, then trackActivities should be true`() {
        val configuration = configurationBuilder.setTrackActivities(true).build()

        assertTrue(configuration.trackActivities)
    }

    @Test
    fun `when setCollectDeviceId is set to false, then collectDeviceId should be false`() {
        val configuration = configurationBuilder.setCollectDeviceId(false).build()

        assertFalse(configuration.collectDeviceId)
    }

    @Test
    fun `when setSessionConfiguration is set with custom values, then sessionConfiguration should be updated`() {
        val customSessionConfig = provideSessionConfiguration(
            automaticSessionTracking = false,
            sessionTimeoutInMillis = CUSTOM_SESSION_TIMEOUT,
        )

        val configuration = configurationBuilder.setSessionConfiguration(customSessionConfig).build()

        assertEquals(customSessionConfig, configuration.sessionConfiguration)
    }

    @Test
    fun `when setLogLevel is set with a custom level, then logLevel should be updated`() {
        val configuration = configurationBuilder.setLogLevel(Logger.LogLevel.VERBOSE).build()

        assertEquals(Logger.LogLevel.VERBOSE, configuration.logLevel)
    }

    @Test
    fun `when setLogger is set with a custom logger, then logger should be updated`() {
        val customLogger = mockk<Logger>()
        val configuration = configurationBuilder.setLogger(customLogger).build()

        assertEquals(customLogger, configuration.logger)
    }

    @Test
    fun `when all custom configurations are set, then the Configuration object should reflect those values`() {
        val customSessionConfig = provideSessionConfiguration(
            automaticSessionTracking = false,
            sessionTimeoutInMillis = CUSTOM_SESSION_TIMEOUT,
        )
        val customPolicies = listOf<FlushPolicy>()

        val actualConfiguration = configurationBuilder
            .setTrackApplicationLifecycleEvents(false)
            .setTrackDeepLinks(false)
            .setTrackActivities(true)
            .setCollectDeviceId(false)
            .setSessionConfiguration(customSessionConfig)
            .setControlPlaneUrl(TEST_CONTROL_PLANE_URL)
            .setFlushPolicies(customPolicies)
            .setGzipEnabled(false)
            .setLogLevel(Logger.LogLevel.DEBUG)
            .build()

        val expectedConfiguration = Configuration(
            application = mockApplication,
            trackApplicationLifecycleEvents = false,
            trackDeepLinks = false,
            trackActivities = true,
            collectDeviceId = false,
            sessionConfiguration = customSessionConfig,
            writeKey = TEST_WRITE_KEY,
            dataPlaneUrl = TEST_DATA_PLANE_URL,
            controlPlaneUrl = TEST_CONTROL_PLANE_URL,
            flushPolicies = customPolicies,
            gzipEnabled = false,
            logger = actualConfiguration.logger,
            logLevel = Logger.LogLevel.DEBUG,
        )
        assertEquals(expectedConfiguration, actualConfiguration)
    }
}
