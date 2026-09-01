package com.rudderstack.sdk.kotlin.core.internals.models.consent

import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementOptions
import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SetConsentActionTest {

    @Test
    fun `given existing consent lists, when new options are reduced, then both lists are fully replaced`() {
        val currentState = ConsentManagementState(
            enabled = true,
            allowedConsentIds = listOf("old-allowed"),
            deniedConsentIds = listOf("old-denied"),
            initialized = true,
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
            enabled = true,
            allowedConsentIds = listOf("marketing"),
            deniedConsentIds = listOf("advertising"),
            initialized = true,
        )
        val action = SetConsentAction(ConsentManagementOptions(allowedConsentIds = listOf("analytics")))

        val newState = action.reduce(currentState)

        assertEquals(listOf("analytics"), newState.allowedConsentIds)
        assertTrue(newState.deniedConsentIds.isEmpty())
    }

    @Test
    fun `given a populated consent state, when empty options are reduced, then the state is unchanged`() {
        val currentState = ConsentManagementState(
            enabled = true,
            allowedConsentIds = listOf("marketing"),
            deniedConsentIds = listOf("advertising"),
            initialized = true,
        )
        val action = SetConsentAction(ConsentManagementOptions())

        val newState = action.reduce(currentState)

        assertEquals(currentState, newState)
    }

    @Test
    fun `given a populated consent state, when options whose ids are only whitespace are reduced, then the state is unchanged`() {
        val currentState = ConsentManagementState(
            enabled = true,
            allowedConsentIds = listOf("marketing"),
            deniedConsentIds = listOf("advertising"),
            initialized = true,
        )
        val action = SetConsentAction(ConsentManagementOptions(allowedConsentIds = listOf("   ", "")))

        val newState = action.reduce(currentState)

        assertEquals(currentState, newState)
    }

    @Test
    fun `given an uninitialized state, when options with consent data are reduced, then the state becomes initialized`() {
        val currentState = ConsentManagementState(enabled = true, initialized = false)
        val action = SetConsentAction(ConsentManagementOptions(deniedConsentIds = listOf("advertising")))

        val newState = action.reduce(currentState)

        assertTrue(newState.initialized)
    }

    @Test
    fun `given options with messy consent ids, when reduced, then the lists are normalized`() {
        val currentState = ConsentManagementState(enabled = true)
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
    fun `given a disabled state, when options with consent data are reduced, then the state is completely unchanged`() {
        val currentState = ConsentManagementState(enabled = false, initialized = false)
        val action = SetConsentAction(ConsentManagementOptions(allowedConsentIds = listOf("marketing")))

        val newState = action.reduce(currentState)

        assertEquals(currentState, newState)
    }

    @Test
    fun `given an enabled state, when any options are reduced, then enabled and provider are untouched`() {
        val currentState = ConsentManagementState(
            enabled = true,
            provider = ConsentManagementProvider.CUSTOM,
            initialized = false,
        )
        val action = SetConsentAction(ConsentManagementOptions(allowedConsentIds = listOf("marketing")))

        val newState = action.reduce(currentState)

        assertTrue(newState.enabled)
        assertEquals(ConsentManagementProvider.CUSTOM, newState.provider)
    }
}
