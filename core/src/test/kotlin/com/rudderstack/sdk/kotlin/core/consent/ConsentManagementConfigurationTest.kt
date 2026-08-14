package com.rudderstack.sdk.kotlin.core.consent

import com.rudderstack.sdk.kotlin.core.Configuration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val TEST_WRITE_KEY = "test-write-key"
private const val TEST_DATA_PLANE_URL = "https://test-data-plane.com"

class ConsentManagementConfigurationTest {

    @Test
    fun `given no consent configuration, when a configuration is created, then consent management is disabled with defaults`() {
        val configuration = Configuration(
            writeKey = TEST_WRITE_KEY,
            dataPlaneUrl = TEST_DATA_PLANE_URL,
        )

        assertFalse(configuration.consentManagement.enabled)
        assertEquals(ConsentManagementProvider.CUSTOM, configuration.consentManagement.provider)
        assertTrue(configuration.consentManagement.allowedConsentIds.isEmpty())
        assertTrue(configuration.consentManagement.deniedConsentIds.isEmpty())
    }

    @Test
    fun `given a full consent configuration, when a configuration is created, then every field round trips`() {
        val consentManagement = ConsentManagementConfiguration(
            enabled = true,
            provider = ConsentManagementProvider.CUSTOM,
            allowedConsentIds = listOf("marketing", "analytics"),
            deniedConsentIds = listOf("advertising"),
        )

        val configuration = Configuration(
            writeKey = TEST_WRITE_KEY,
            dataPlaneUrl = TEST_DATA_PLANE_URL,
            consentManagement = consentManagement,
        )

        assertTrue(configuration.consentManagement.enabled)
        assertEquals(ConsentManagementProvider.CUSTOM, configuration.consentManagement.provider)
        assertEquals(listOf("marketing", "analytics"), configuration.consentManagement.allowedConsentIds)
        assertEquals(listOf("advertising"), configuration.consentManagement.deniedConsentIds)
    }

    @Test
    fun `given no arguments, when a consent management configuration is created, then defaults are applied`() {
        val consentManagement = ConsentManagementConfiguration()

        assertFalse(consentManagement.enabled)
        assertEquals(ConsentManagementProvider.CUSTOM, consentManagement.provider)
        assertTrue(consentManagement.allowedConsentIds.isEmpty())
        assertTrue(consentManagement.deniedConsentIds.isEmpty())
    }

    @Test
    fun `given the custom provider, when its wire value is read, then it serializes as custom`() {
        assertEquals("custom", ConsentManagementProvider.CUSTOM.value)
    }

    @Test
    fun `given a configuration with consent ids, when converted to string, then the ids are redacted`() {
        val consentManagement = ConsentManagementConfiguration(
            enabled = true,
            allowedConsentIds = listOf("marketing"),
            deniedConsentIds = listOf("advertising"),
        )

        val result = consentManagement.toString()

        assertFalse(result.contains("marketing"))
        assertFalse(result.contains("advertising"))
        assertTrue(result.contains("allowedConsentIds=1 id(s)"))
        assertTrue(result.contains("deniedConsentIds=1 id(s)"))
    }
}
