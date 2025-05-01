package com.rudderstack.sdk.kotlin.android.javacompat

import com.rudderstack.sdk.kotlin.android.DEFAULT_SESSION_TIMEOUT_IN_MILLIS
import com.rudderstack.sdk.kotlin.android.SessionConfiguration
import com.rudderstack.sdk.kotlin.android.SessionConfiguration.Companion.DEFAULT_AUTOMATIC_SESSION_TRACKING
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
}

private fun provideSessionConfiguration(
    automaticSessionTracking: Boolean = DEFAULT_AUTOMATIC_SESSION_TRACKING,
    sessionTimeoutInMillis: Long = DEFAULT_SESSION_TIMEOUT_IN_MILLIS,
): SessionConfiguration {
    return SessionConfiguration(
        automaticSessionTracking = automaticSessionTracking,
        sessionTimeoutInMillis = sessionTimeoutInMillis
    )
}
