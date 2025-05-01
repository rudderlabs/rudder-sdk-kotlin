package com.rudderstack.sdk.kotlin.core.javacompat

import com.rudderstack.sdk.kotlin.core.Configuration
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.policies.FlushPolicy
import io.mockk.MockKAnnotations
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConfigurationBuilderTest {

    private lateinit var configurationBuilder: ConfigurationBuilder
    private val testWriteKey = "test-write-key"
    private val testDataPlaneUrl = "https://test-data-plane.com"

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        configurationBuilder = ConfigurationBuilder(testWriteKey, testDataPlaneUrl)
    }

    @Test
    fun `when setControlPlaneUrl is set with a custom URL, then controlPlaneUrl should be updated`() {
        val testControlPlaneUrl = "https://test-control-plane.com"
        val config = configurationBuilder.setControlPlaneUrl(testControlPlaneUrl).build()

        assertEquals(testControlPlaneUrl, config.controlPlaneUrl)
    }

    @Test
    fun `when setLogLevel is set with a custom level, then logLevel should be updated`() {
        val config = configurationBuilder.setLogLevel(Logger.LogLevel.VERBOSE).build()

        assertEquals(Logger.LogLevel.VERBOSE, config.logLevel)
    }

    @Test
    fun `when setFlushPolicies is set with custom policies, then flushPolicies should be updated`() {
        val customPolicies = listOf<FlushPolicy>()
        val config = configurationBuilder.setFlushPolicies(customPolicies).build()

        assertEquals(customPolicies, config.flushPolicies)
    }

    @Test
    fun `when setGzipEnabled is set to false, then gzipEnabled should be false`() {
        val config = configurationBuilder.setGzipEnabled(false).build()

        assertFalse(config.gzipEnabled)
    }

    @Test
    fun `when all custom configurations are set, then the Configuration object should reflect those values`() {
        val customPolicies = listOf<FlushPolicy>()
        val testControlPlaneUrl = "https://test-control-plane.com"

        val actualConfiguration = configurationBuilder
            .setControlPlaneUrl(testControlPlaneUrl)
            .setLogLevel(Logger.LogLevel.NONE)
            .setFlushPolicies(customPolicies)
            .setGzipEnabled(false)
            .build()

        val expectedConfiguration = Configuration(
            writeKey = testWriteKey,
            dataPlaneUrl = testDataPlaneUrl,
            controlPlaneUrl = testControlPlaneUrl,
            logLevel = Logger.LogLevel.NONE,
            flushPolicies = customPolicies,
            gzipEnabled = false,
        )

        // As core, Configuration class is not a data class, we cannot use assertEquals directly
        with(actualConfiguration) {
            assertEquals(expectedConfiguration.writeKey, writeKey)
            assertEquals(expectedConfiguration.dataPlaneUrl, dataPlaneUrl)
            assertEquals(expectedConfiguration.controlPlaneUrl, controlPlaneUrl)
            assertEquals(expectedConfiguration.logLevel, logLevel)
            assertEquals(expectedConfiguration.flushPolicies, flushPolicies)
            assertEquals(expectedConfiguration.gzipEnabled, gzipEnabled)
        }
    }
}

