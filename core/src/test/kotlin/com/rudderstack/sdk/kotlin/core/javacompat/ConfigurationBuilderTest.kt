package com.rudderstack.sdk.kotlin.core.javacompat

import com.rudderstack.sdk.kotlin.core.Configuration
import com.rudderstack.sdk.kotlin.core.Configuration.Companion.DEFAULT_CONTROL_PLANE_URL
import com.rudderstack.sdk.kotlin.core.Configuration.Companion.DEFAULT_FLUSH_POLICIES
import com.rudderstack.sdk.kotlin.core.Configuration.Companion.DEFAULT_GZIP_STATUS
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.policies.FlushPolicy
import io.mockk.MockKAnnotations
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val TEST_WRITE_KEY = "test-write-key"
private const val TEST_DATA_PLANE_URL = "https://test-data-plane.com"
private const val TEST_CONTROL_PLANE_URL = "https://test-control-plane.com"

class ConfigurationBuilderTest {

    private lateinit var configurationBuilder: ConfigurationBuilder

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        configurationBuilder = ConfigurationBuilder(TEST_WRITE_KEY, TEST_DATA_PLANE_URL)
    }

    @Test
    fun `when Configuration object is created with only default values, then it should have default values`() {
        val configuration = configurationBuilder.build()

        // As core, Configuration class is not a data class, we cannot use assertEquals directly
        configuration.also {
            assertEquals(TEST_WRITE_KEY, it.writeKey)
            assertEquals(TEST_DATA_PLANE_URL, it.dataPlaneUrl)
            assertEquals(DEFAULT_CONTROL_PLANE_URL, it.controlPlaneUrl)
            assertEquals(Logger.DEFAULT_LOG_LEVEL, it.logLevel)
            assertEquals(DEFAULT_FLUSH_POLICIES, it.flushPolicies)
            assertEquals(DEFAULT_GZIP_STATUS, it.gzipEnabled)
        }
    }

    @Test
    fun `when setControlPlaneUrl is set with a custom URL, then controlPlaneUrl should be updated`() {
        val config = configurationBuilder.setControlPlaneUrl(TEST_CONTROL_PLANE_URL).build()

        assertEquals(TEST_CONTROL_PLANE_URL, config.controlPlaneUrl)
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

        val actualConfiguration = configurationBuilder
            .setControlPlaneUrl(TEST_CONTROL_PLANE_URL)
            .setLogLevel(Logger.LogLevel.NONE)
            .setFlushPolicies(customPolicies)
            .setGzipEnabled(false)
            .build()

        val expectedConfiguration = Configuration(
            writeKey = TEST_WRITE_KEY,
            dataPlaneUrl = TEST_DATA_PLANE_URL,
            controlPlaneUrl = TEST_CONTROL_PLANE_URL,
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

