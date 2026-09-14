package com.rudderstack.sdk.kotlin.android.javacompat

import com.rudderstack.sdk.kotlin.android.consent.ConsentManagementOptions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConsentManagementOptionsBuilderTest {

    @Test
    fun `when no setters are called, then the built options carry no consent ids`() {
        val options = ConsentManagementOptionsBuilder().build()

        assertEquals(ConsentManagementOptions(), options)
        assertTrue(options.allowedConsentIds.isEmpty())
        assertTrue(options.deniedConsentIds.isEmpty())
    }

    @Test
    fun `when every setter is called, then all fields round trip into the built options`() {
        val options = ConsentManagementOptionsBuilder()
            .setAllowedConsentIds(listOf("marketing", "analytics"))
            .setDeniedConsentIds(listOf("advertising"))
            .build()

        assertEquals(listOf("marketing", "analytics"), options.allowedConsentIds)
        assertEquals(listOf("advertising"), options.deniedConsentIds)
    }

    @Test
    fun `when only the denied ids are set, then the allowed ids stay empty`() {
        val options = ConsentManagementOptionsBuilder()
            .setDeniedConsentIds(listOf("advertising"))
            .build()

        assertTrue(options.allowedConsentIds.isEmpty())
        assertEquals(listOf("advertising"), options.deniedConsentIds)
    }
}
