package com.rudderstack.sdk.kotlin.android.javacompat

import com.rudderstack.sdk.kotlin.android.utils.provideSessionConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val CUSTOM_TIME_IN_MILLIS = 45000L

class SessionConfigurationBuilderTest {

    private lateinit var sessionConfigurationBuilder: SessionConfigurationBuilder

    @BeforeEach
    fun setUp() {
        sessionConfigurationBuilder = SessionConfigurationBuilder()
    }

    @Test
    fun `when SessionConfiguration object is created with only default values, then it should have default values`() {
        val sessionConfiguration = sessionConfigurationBuilder.build()

        val expected = provideSessionConfiguration()
        assertEquals(expected, sessionConfiguration)
    }

    @Test
    fun `when setAutomaticSessionTracking is set to false, then automaticSessionTracking should be false`() {
        val sessionConfiguration = sessionConfigurationBuilder.setAutomaticSessionTracking(false).build()

        assertFalse(sessionConfiguration.automaticSessionTracking)
    }

    @Test
    fun `when setSessionTimeoutInMillis is set with custom value, then sessionTimeoutInMillis should be updated`() {
        val sessionConfiguration = sessionConfigurationBuilder.setSessionTimeoutInMillis(CUSTOM_TIME_IN_MILLIS).build()

        assertEquals(CUSTOM_TIME_IN_MILLIS, sessionConfiguration.sessionTimeoutInMillis)
    }

    @Test
    fun `when all custom configurations are set, then SessionConfiguration should reflect those values`() {
        val config = sessionConfigurationBuilder
            .setAutomaticSessionTracking(false)
            .setSessionTimeoutInMillis(CUSTOM_TIME_IN_MILLIS)
            .build()

        val expected =
            provideSessionConfiguration(sessionTimeoutInMillis = CUSTOM_TIME_IN_MILLIS, automaticSessionTracking = false)
        assertEquals(expected, config)
    }

    @Test
    fun `when SessionConfiguration is built with default values, then updateSessionOnBackgroundEvents should be false`() {
        val sessionConfiguration = sessionConfigurationBuilder.build()

        assertFalse(sessionConfiguration.updateSessionOnBackgroundEvents)
    }

    @Test
    fun `when setUpdateSessionOnBackgroundEvents is set to true, then updateSessionOnBackgroundEvents should be true`() {
        val sessionConfiguration = sessionConfigurationBuilder.setUpdateSessionOnBackgroundEvents(true).build()

        assertTrue(sessionConfiguration.updateSessionOnBackgroundEvents)
    }
}
