@file:Suppress("DEPRECATION")

package com.rudderstack.sdk.kotlin.android

import android.app.Application
import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementConfiguration
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val TEST_WRITE_KEY = "test-write-key"
private const val TEST_DATA_PLANE_URL = "https://test-data-plane.com"

class ConfigurationTest {

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockLogger: Logger

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        mockkObject(LoggerAnalytics)
        every { LoggerAnalytics.logger } returns mockLogger
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given no consent configuration, when an android configuration is created, then it inherits the disabled default`() {
        val configuration = Configuration(
            application = mockApplication,
            writeKey = TEST_WRITE_KEY,
            dataPlaneUrl = TEST_DATA_PLANE_URL,
        )

        assertEquals(ConsentManagementConfiguration(), configuration.consentManagement)
        assertFalse(configuration.consentManagement.enabled)
    }

    @Test
    fun `given a consent configuration, when an android configuration is created, then it inherits the consent management field`() {
        val consentManagement = ConsentManagementConfiguration(
            enabled = true,
            allowedConsentIds = listOf("marketing"),
            deniedConsentIds = listOf("advertising"),
        )

        val configuration = Configuration(
            application = mockApplication,
            writeKey = TEST_WRITE_KEY,
            dataPlaneUrl = TEST_DATA_PLANE_URL,
            consentManagement = consentManagement,
        )

        assertEquals(consentManagement, configuration.consentManagement)
    }
}
