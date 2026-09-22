package com.rudderstack.sdk.kotlin.android.models.consent

import com.rudderstack.sdk.kotlin.android.consent.ConsentManagementOptions
import com.rudderstack.sdk.kotlin.android.consent.ConsentManagementProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SetConsentActionTest {

    @Test
    fun `given existing consent lists, when new options are reduced, then both lists are fully replaced`() {
        val currentState = ConsentManagementState(
            active = true,
            allowedConsentIds = listOf("old-allowed"),
            deniedConsentIds = listOf("old-denied"),
        )
        val action = SetConsentAction(
            ConsentManagementOptions(
                allowedConsentIds = listOf("marketing"),
                deniedConsentIds = listOf("advertising"),
            )
        )

        val newState = action.reduce(currentState)

        assertEquals(listOf("marketing"), newState.allowedConsentIds)
        assertEquals(listOf("advertising"), newState.deniedConsentIds)
    }

    @Test
    fun `given both lists populated, when options carrying only an allowed list are reduced, then the omitted denied list is cleared`() {
        val currentState = ConsentManagementState(
            active = true,
            allowedConsentIds = listOf("marketing"),
            deniedConsentIds = listOf("advertising"),
        )
        val action = SetConsentAction(ConsentManagementOptions(allowedConsentIds = listOf("analytics")))

        val newState = action.reduce(currentState)

        assertEquals(listOf("analytics"), newState.allowedConsentIds)
        assertTrue(newState.deniedConsentIds.isEmpty())
    }

    @Test
    fun `given empty options, when reduced, then both lists are cleared because validation belongs to the caller`() {
        val currentState = ConsentManagementState(
            active = true,
            allowedConsentIds = listOf("marketing"),
            deniedConsentIds = listOf("advertising"),
        )
        val action = SetConsentAction(ConsentManagementOptions())

        val newState = action.reduce(currentState)

        assertTrue(newState.allowedConsentIds.isEmpty())
        assertTrue(newState.deniedConsentIds.isEmpty())
    }

    @Test
    fun `given a state with no consent lists, when options with consent data are reduced, then the lists are applied`() {
        val currentState = ConsentManagementState(active = true)
        val action = SetConsentAction(ConsentManagementOptions(deniedConsentIds = listOf("advertising")))

        val newState = action.reduce(currentState)

        assertTrue(newState.allowedConsentIds.isEmpty())
        assertEquals(listOf("advertising"), newState.deniedConsentIds)
    }

    @Test
    fun `given options with messy consent ids, when reduced, then the lists are normalized`() {
        val currentState = ConsentManagementState(active = true)
        val action = SetConsentAction(
            ConsentManagementOptions(
                allowedConsentIds = listOf(" marketing ", ""),
                deniedConsentIds = listOf("   ", "advertising"),
            )
        )

        val newState = action.reduce(currentState)

        assertEquals(listOf("marketing"), newState.allowedConsentIds)
        assertEquals(listOf("advertising"), newState.deniedConsentIds)
    }

    @Test
    fun `given an inactive state, when options with consent data are reduced, then the state is completely unchanged`() {
        val currentState = ConsentManagementState(active = false)
        val action = SetConsentAction(ConsentManagementOptions(allowedConsentIds = listOf("marketing")))

        val newState = action.reduce(currentState)

        assertEquals(currentState, newState)
    }

    @Test
    fun `given an active state, when any options are reduced, then active and provider are untouched`() {
        val currentState = ConsentManagementState(
            active = true,
            provider = ConsentManagementProvider.CUSTOM,
        )
        val action = SetConsentAction(ConsentManagementOptions(allowedConsentIds = listOf("marketing")))

        val newState = action.reduce(currentState)

        assertTrue(newState.active)
        assertEquals(ConsentManagementProvider.CUSTOM, newState.provider)
    }
}
