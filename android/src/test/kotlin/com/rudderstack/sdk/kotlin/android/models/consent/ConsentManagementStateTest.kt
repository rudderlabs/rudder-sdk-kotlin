package com.rudderstack.sdk.kotlin.android.models.consent

import com.rudderstack.sdk.kotlin.android.consent.ConsentManagementConfiguration
import com.rudderstack.sdk.kotlin.android.consent.ConsentManagementProvider
import com.rudderstack.sdk.kotlin.android.models.consent.ConsentManagementState.Companion.normalized
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
    fun `given an enabled configuration with a non-empty list, when the initial state is built, then consent management is active`() {
        val configuration = ConsentManagementConfiguration(enabled = true, allowedConsentIds = listOf("marketing"))

        val state = ConsentManagementState.initialState(configuration)

        assertTrue(state.active)
    }

    @Test
    fun `given an enabled configuration with both lists empty, when the initial state is built, then consent management is inactive`() {
        val configuration = ConsentManagementConfiguration(enabled = true)

        val state = ConsentManagementState.initialState(configuration)

        assertFalse(state.active)
    }

    @Test
    fun `given an enabled configuration whose ids are only whitespace, when the initial state is built, then consent management is inactive`() {
        val configuration = ConsentManagementConfiguration(enabled = true, allowedConsentIds = listOf("   ", ""))

        val state = ConsentManagementState.initialState(configuration)

        assertFalse(state.active)
    }

    @Test
    fun `given a disabled configuration carrying consent lists, when the initial state is built, then consent management is inactive`() {
        val configuration = ConsentManagementConfiguration(
            enabled = false,
            allowedConsentIds = listOf("marketing"),
            deniedConsentIds = listOf("advertising"),
        )

        val state = ConsentManagementState.initialState(configuration)

        assertFalse(state.active)
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

    // Stamp round-trip
    //
    // The stamp is what an event carries from creation to delivery, and the gate reads it back to
    // judge the event against the decision it was created under. If the two operations disagree,
    // that judgement is made against something the user never chose.

    @Test
    fun `given an active state, when its stamp is read back, then the state round-trips unchanged`() {
        val state = ConsentManagementState(
            active = true,
            provider = ConsentManagementProvider.CUSTOM,
            allowedConsentIds = listOf("marketing", "analytics"),
            deniedConsentIds = listOf("advertising"),
        )

        assertEquals(state, state.consentStamp.toConsentManagementState())
    }

    @Test
    fun `given a stamp naming an unknown provider, when it is read back, then it falls back to the supported provider`() {
        val stamp = buildJsonObject {
            put("provider", "some-future-cmp")
            put("allowedConsentIds", buildJsonArray { add("marketing") })
            put("deniedConsentIds", buildJsonArray { })
        }

        val state = stamp.toConsentManagementState()

        assertEquals(ConsentManagementProvider.CUSTOM, state.provider)
        assertEquals(listOf("marketing"), state.allowedConsentIds)
        assertTrue(state.active)
    }
}
