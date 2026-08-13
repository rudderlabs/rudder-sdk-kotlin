package com.rudderstack.sdk.kotlin.core.javacompat

import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementConfiguration
import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConsentManagementConfigurationBuilderTest {

    @Test
    fun `when no setters are called, then the built configuration has the disabled defaults`() {
        val consentManagement = ConsentManagementConfigurationBuilder().build()

        assertEquals(ConsentManagementConfiguration(), consentManagement)
        assertFalse(consentManagement.enabled)
        assertEquals(ConsentManagementProvider.CUSTOM, consentManagement.provider)
        assertTrue(consentManagement.allowedConsentIds.isEmpty())
        assertTrue(consentManagement.deniedConsentIds.isEmpty())
    }

    @Test
    fun `when every setter is called, then all fields round trip into the built configuration`() {
        val consentManagement = ConsentManagementConfigurationBuilder()
            .setEnabled(true)
            .setProvider(ConsentManagementProvider.CUSTOM)
            .setAllowedConsentIds(listOf("marketing", "analytics"))
            .setDeniedConsentIds(listOf("advertising"))
            .build()

        assertTrue(consentManagement.enabled)
        assertEquals(ConsentManagementProvider.CUSTOM, consentManagement.provider)
        assertEquals(listOf("marketing", "analytics"), consentManagement.allowedConsentIds)
        assertEquals(listOf("advertising"), consentManagement.deniedConsentIds)
    }
}
