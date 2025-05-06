package com.rudderstack.sdk.kotlin.android.javacompat

import android.app.Application
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.utils.provideSessionConfiguration
import com.rudderstack.sdk.kotlin.core.internals.policies.FlushPolicy
import com.rudderstack.sdk.kotlin.core.provideListOfFlushPolicies
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
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
        every { provideListOfFlushPolicies() } returns mockPolicies

        configurationBuilder = ConfigurationBuilder(
            mockApplication,
            TEST_WRITE_KEY, TEST_DATA_PLANE_URL
        )
    }

    @Test
    fun `when Configuration object is created with only default values, then it should have default values`() {
        val configuration = configurationBuilder.build()

        val expected =
            Configuration(application = mockApplication, writeKey = TEST_WRITE_KEY, dataPlaneUrl = TEST_DATA_PLANE_URL)
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
        )
        assertEquals(expectedConfiguration, actualConfiguration)
    }
}
