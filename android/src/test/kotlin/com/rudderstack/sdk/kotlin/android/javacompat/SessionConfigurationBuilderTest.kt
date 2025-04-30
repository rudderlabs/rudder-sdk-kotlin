package com.rudderstack.sdk.kotlin.android.javacompat

import com.rudderstack.sdk.kotlin.android.DEFAULT_SESSION_TIMEOUT_IN_MILLIS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SessionConfigurationBuilderTest {

    private lateinit var builder: SessionConfigurationBuilder

    @BeforeEach
    fun setUp() {
        builder = SessionConfigurationBuilder()
    }

    @Test
    fun `when SessionConfiguration object is created with only default values, then it should have default values`() {
        val config = builder.build()

        assertTrue(config.automaticSessionTracking)
        assertEquals(DEFAULT_SESSION_TIMEOUT_IN_MILLIS, config.sessionTimeoutInMillis)
    }

    @Test
    fun `when withAutomaticSessionTracking is set to false, then automaticSessionTracking should be false`() {
        val config = builder.withAutomaticSessionTracking(false).build()

        assertFalse(config.automaticSessionTracking)
    }

    @Test
    fun `when withSessionTimeoutInMillis is set with custom value, then sessionTimeoutInMillis should be updated`() {
        val customTimeout = 30000L
        val config = builder.withSessionTimeoutInMillis(customTimeout).build()

        assertEquals(customTimeout, config.sessionTimeoutInMillis)
    }

    @Test
    fun `when all custom configurations are set, then SessionConfiguration should reflect those values`() {
        val customTimeout = 45000L
        val config = builder
            .withAutomaticSessionTracking(false)
            .withSessionTimeoutInMillis(customTimeout)
            .build()

        assertFalse(config.automaticSessionTracking)
        assertEquals(customTimeout, config.sessionTimeoutInMillis)
    }
}
