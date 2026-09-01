package com.rudderstack.sdk.kotlin.core.internals.models.consent

import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementConfiguration
import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementProvider
import com.rudderstack.sdk.kotlin.core.internals.models.consent.ConsentManagementState.Companion.normalized
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConsentManagementStateTest {

    // Normalizer

    @Test
    fun `given consent ids with surrounding whitespace, when normalized, then each id is trimmed`() {
        val normalized = listOf(" marketing ", "\tanalytics\n").normalized()

        assertEquals(listOf("marketing", "analytics"), normalized)
    }

    @Test
    fun `given consent ids containing empty or blank entries, when normalized, then those entries are dropped`() {
        val normalized = listOf("", "   ", "analytics").normalized()

        assertEquals(listOf("analytics"), normalized)
    }

    // Initial state

    @Test
    fun `given an enabled configuration with a non empty list, when the initial state is built, then it is initialized`() {
        val configuration = ConsentManagementConfiguration(enabled = true, allowedConsentIds = listOf("marketing"))

        val state = ConsentManagementState.initialState(configuration)

        assertTrue(state.enabled)
        assertTrue(state.initialized)
    }

    @Test
    fun `given an enabled configuration with both lists empty, when the initial state is built, then consent management is inactive`() {
        val configuration = ConsentManagementConfiguration(enabled = true)

        val state = ConsentManagementState.initialState(configuration)

        assertFalse(state.enabled)
    }

    @Test
    fun `given an enabled configuration whose ids are only whitespace, when the initial state is built, then consent management is inactive`() {
        val configuration = ConsentManagementConfiguration(enabled = true, allowedConsentIds = listOf("   ", ""))

        val state = ConsentManagementState.initialState(configuration)

        assertFalse(state.enabled)
    }

    @Test
    fun `given a disabled configuration carrying consent lists, when the initial state is built, then it stays uninitialized`() {
        val configuration = ConsentManagementConfiguration(
            enabled = false,
            allowedConsentIds = listOf("marketing"),
            deniedConsentIds = listOf("advertising"),
        )

        val state = ConsentManagementState.initialState(configuration)

        assertFalse(state.enabled)
        assertFalse(state.initialized)
    }

    @Test
    fun `given a configuration with messy consent ids, when the initial state is built, then the lists are normalized`() {
        val configuration = ConsentManagementConfiguration(
            enabled = true,
            allowedConsentIds = listOf(" marketing ", ""),
            deniedConsentIds = listOf("   ", "advertising"),
        )

        val state = ConsentManagementState.initialState(configuration)

        assertEquals(listOf("marketing"), state.allowedConsentIds)
        assertEquals(listOf("advertising"), state.deniedConsentIds)
    }

    @Test
    fun `given a full configuration, when the initial state is built, then provider and lists are copied over`() {
        val configuration = ConsentManagementConfiguration(
            enabled = true,
            provider = ConsentManagementProvider.CUSTOM,
            allowedConsentIds = listOf("marketing"),
            deniedConsentIds = listOf("advertising"),
        )

        val state = ConsentManagementState.initialState(configuration)

        assertEquals(ConsentManagementProvider.CUSTOM, state.provider)
        assertEquals(listOf("marketing"), state.allowedConsentIds)
        assertEquals(listOf("advertising"), state.deniedConsentIds)
    }
}
