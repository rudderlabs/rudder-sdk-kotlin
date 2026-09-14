package com.rudderstack.sdk.kotlin.android.consent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConsentManagementOptionsTest {

    @Test
    fun `given two instances carrying the same consent ids, when they are compared, then they are equal`() {
        val first = ConsentManagementOptions(
            allowedConsentIds = listOf("marketing", "analytics"),
            deniedConsentIds = listOf("advertising"),
        )
        val second = ConsentManagementOptions(
            allowedConsentIds = listOf("marketing", "analytics"),
            deniedConsentIds = listOf("advertising"),
        )

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `given two instances with the lists swapped, when they are compared, then they are not equal`() {
        val granted = ConsentManagementOptions(allowedConsentIds = listOf("marketing"))
        val refused = ConsentManagementOptions(deniedConsentIds = listOf("marketing"))

        assertNotEquals(granted, refused)
    }

    @Test
    fun `given options with consent ids, when converted to string, then the ids are redacted`() {
        val options = ConsentManagementOptions(
            allowedConsentIds = listOf("marketing"),
            deniedConsentIds = listOf("advertising"),
        )

        val result = options.toString()

        assertFalse(result.contains("marketing"))
        assertFalse(result.contains("advertising"))
        assertTrue(result.contains("allowedConsentIds=1 id(s)"))
        assertTrue(result.contains("deniedConsentIds=1 id(s)"))
    }
}
