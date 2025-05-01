package com.rudderstack.sdk.kotlin.android.javacompat

import android.app.Application
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.SessionConfiguration
import com.rudderstack.sdk.kotlin.core.Configuration.Companion.DEFAULT_CONTROL_PLANE_URL
import com.rudderstack.sdk.kotlin.core.Configuration.Companion.DEFAULT_FLUSH_POLICIES
import com.rudderstack.sdk.kotlin.core.Configuration.Companion.DEFAULT_GZIP_STATUS
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.policies.FlushPolicy
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConfigurationBuilderTest {

    @MockK
    private lateinit var mockApplication: Application

    private lateinit var configurationBuilder: ConfigurationBuilder
    private val testWriteKey = "test-write-key"
    private val testDataPlaneUrl = "https://test-data-plane.com"

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        configurationBuilder = ConfigurationBuilder(mockApplication, testWriteKey, testDataPlaneUrl)
    }

    @Test
    fun `when Configuration object is created with only default values, then it should have default values`() {
        val config = configurationBuilder.build()

        assertEquals(mockApplication, config.application)
        assertEquals(testWriteKey, config.writeKey)
        assertEquals(testDataPlaneUrl, config.dataPlaneUrl)
        assertEquals(DEFAULT_CONTROL_PLANE_URL, config.controlPlaneUrl)
        assertTrue(config.trackApplicationLifecycleEvents)
        assertTrue(config.trackDeepLinks)
        assertFalse(config.trackActivities)
        assertTrue(config.collectDeviceId)
        assertEquals(Logger.DEFAULT_LOG_LEVEL, config.logLevel)
        assertEquals(DEFAULT_FLUSH_POLICIES, config.flushPolicies)
        assertEquals(DEFAULT_GZIP_STATUS, config.gzipEnabled)
    }

    @Test
    fun `when setTrackApplicationLifecycleEvents is set to false, then trackApplicationLifecycleEvents should be false`() {
        val config = configurationBuilder.setTrackApplicationLifecycleEvents(false).build()

        assertFalse(config.trackApplicationLifecycleEvents)
    }

    @Test
    fun `when setTrackDeepLinks is set to false, then trackDeepLinks should be false`() {
        val config = configurationBuilder.setTrackDeepLinks(false).build()

        assertFalse(config.trackDeepLinks)
    }

    @Test
    fun `when setTrackActivities is set to true, then trackActivities should be true`() {
        val config = configurationBuilder.setTrackActivities(true).build()

        assertTrue(config.trackActivities)
    }

    @Test
    fun `when setCollectDeviceId is set to false, then collectDeviceId should be false`() {
        val config = configurationBuilder.setCollectDeviceId(false).build()

        assertFalse(config.collectDeviceId)
    }

    @Test
    fun `when setSessionConfiguration is set with custom values, then sessionConfiguration should be updated`() {
        val customSessionConfig = SessionConfiguration(
            automaticSessionTracking = false,
            sessionTimeoutInMillis = 60000L
        )
        val config = configurationBuilder.setSessionConfiguration(customSessionConfig).build()

        assertEquals(customSessionConfig, config.sessionConfiguration)
        assertFalse(config.sessionConfiguration.automaticSessionTracking)
        assertEquals(60000L, config.sessionConfiguration.sessionTimeoutInMillis)
    }

    @Test
    fun `when all custom configurations are set, then the Configuration object should reflect those values`() {
        val customSessionConfig = SessionConfiguration(
            automaticSessionTracking = false,
            sessionTimeoutInMillis = 30000L
        )
        val customPolicies = listOf<FlushPolicy>()
        val testControlPlaneUrl = "https://test-control-plane.com"

        val actualConfiguration = configurationBuilder
            .setTrackApplicationLifecycleEvents(false)
            .setTrackDeepLinks(false)
            .setTrackActivities(true)
            .setCollectDeviceId(false)
            .setSessionConfiguration(customSessionConfig)
            .setControlPlaneUrl(testControlPlaneUrl)
            .setLogLevel(Logger.LogLevel.NONE)
            .setFlushPolicies(customPolicies)
            .setGzipEnabled(false)
            .build()

        val expectedConfiguration = Configuration(
            application = mockApplication,
            trackApplicationLifecycleEvents = false,
            trackDeepLinks = false,
            trackActivities = true,
            collectDeviceId = false,
            sessionConfiguration = customSessionConfig,
            writeKey = testWriteKey,
            dataPlaneUrl = testDataPlaneUrl,
            controlPlaneUrl = testControlPlaneUrl,
            logLevel = Logger.LogLevel.NONE,
            flushPolicies = customPolicies,
            gzipEnabled = false,
        )
        assertEquals(expectedConfiguration, actualConfiguration)
    }
}
