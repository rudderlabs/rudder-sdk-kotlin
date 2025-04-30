package com.rudderstack.sdk.kotlin.android.javacompat

import android.app.Application
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

    private lateinit var builder: ConfigurationBuilder
    private val testWriteKey = "test-write-key"
    private val testDataPlaneUrl = "https://test-data-plane.com"

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        builder = ConfigurationBuilder(mockApplication, testWriteKey, testDataPlaneUrl)
    }

    @Test
    fun `when Configuration object is created with only default values, then it should have default values`() {
        val config = builder.build()

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
    fun `when withTrackApplicationLifecycleEvents is set to false, then trackApplicationLifecycleEvents should be false`() {
        val config = builder.withTrackApplicationLifecycleEvents(false).build()

        assertFalse(config.trackApplicationLifecycleEvents)
    }

    @Test
    fun `when withTrackDeepLinks is set to false, then trackDeepLinks should be false`() {
        val config = builder.withTrackDeepLinks(false).build()

        assertFalse(config.trackDeepLinks)
    }

    @Test
    fun `when withTrackActivities is set to true, then trackActivities should be true`() {
        val config = builder.withTrackActivities(true).build()

        assertTrue(config.trackActivities)
    }

    @Test
    fun `when withCollectDeviceId is set to false, then collectDeviceId should be false`() {
        val config = builder.withCollectDeviceId(false).build()

        assertFalse(config.collectDeviceId)
    }

    @Test
    fun `when withSessionConfiguration is set with custom values, then sessionConfiguration should be updated`() {
        val customSessionConfig = SessionConfiguration(
            automaticSessionTracking = false,
            sessionTimeoutInMillis = 60000L
        )
        val config = builder.withSessionConfiguration(customSessionConfig).build()

        assertEquals(customSessionConfig, config.sessionConfiguration)
        assertFalse(config.sessionConfiguration.automaticSessionTracking)
        assertEquals(60000L, config.sessionConfiguration.sessionTimeoutInMillis)
    }

    @Test
    fun `when withControlPlaneUrl is set with a custom URL, then controlPlaneUrl should be updated`() {
        val testControlPlaneUrl = "https://test-control-plane.com"
        val config = builder.withControlPlaneUrl(testControlPlaneUrl).build()

        assertEquals(testControlPlaneUrl, config.controlPlaneUrl)
    }

    @Test
    fun `when withLogLevel is set with a custom level, then logLevel should be updated`() {
        val config = builder.withLogLevel(Logger.LogLevel.VERBOSE).build()
        assertEquals(Logger.LogLevel.VERBOSE, config.logLevel)
    }

    @Test
    fun `when withFlushPolicies is set with custom policies, then flushPolicies should be updated`() {
        val customPolicies = listOf<FlushPolicy>()
        val config = builder.withFlushPolicies(customPolicies).build()

        assertEquals(customPolicies, config.flushPolicies)
    }

    @Test
    fun `when withGzipEnabled is set to false, then gzipEnabled should be false`() {
        val config = builder.withGzipEnabled(false).build()

        assertFalse(config.gzipEnabled)
    }

    @Test
    fun `when all custom configurations are set, then the Configuration object should reflect those values`() {
        val customSessionConfig = SessionConfiguration(
            automaticSessionTracking = false,
            sessionTimeoutInMillis = 30000L
        )
        val customPolicies = listOf<FlushPolicy>()
        val testControlPlaneUrl = "https://test-control-plane.com"

        val config = builder
            .withTrackApplicationLifecycleEvents(false)
            .withTrackDeepLinks(false)
            .withTrackActivities(true)
            .withCollectDeviceId(false)
            .withSessionConfiguration(customSessionConfig)
            .withControlPlaneUrl(testControlPlaneUrl)
            .withLogLevel(Logger.LogLevel.NONE)
            .withFlushPolicies(customPolicies)
            .withGzipEnabled(false)
            .build()

        assertEquals(mockApplication, config.application)
        assertEquals(testWriteKey, config.writeKey)
        assertEquals(testDataPlaneUrl, config.dataPlaneUrl)
        assertEquals(testControlPlaneUrl, config.controlPlaneUrl)
        assertFalse(config.trackApplicationLifecycleEvents)
        assertFalse(config.trackDeepLinks)
        assertTrue(config.trackActivities)
        assertFalse(config.collectDeviceId)
        assertEquals(Logger.LogLevel.NONE, config.logLevel)
        assertEquals(customPolicies, config.flushPolicies)
        assertFalse(config.gzipEnabled)
        assertEquals(customSessionConfig, config.sessionConfiguration)
    }
}
