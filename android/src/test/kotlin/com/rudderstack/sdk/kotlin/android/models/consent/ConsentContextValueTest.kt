package com.rudderstack.sdk.kotlin.android.models.consent

import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.consent.ConsentManagementProvider
import com.rudderstack.sdk.kotlin.android.plugins.ConsentManagementPlugin
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SDKManagedContextKey
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val EVENT_NAME = "Sample Event"

/**
 * The android half of the terminal re-stamp.
 *
 * `SchemaGuardPlugin` lives in core and re-asserts whatever this supplier returns; core's own
 * tests cover that mechanism with a stub. What belongs here is the consent binding: the value
 * asserted, when nothing is asserted, and that it matches what the early stamper writes.
 */
class ConsentContextValueTest {

    @MockK
    private lateinit var mockAnalytics: Analytics

    private lateinit var reservedValue: ConsentContextValue

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        reservedValue = ConsentContextValue(mockAnalytics)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given consent management enabled, when the reserved value is read, then it is the inner consent stamp`() {
        val state = stubConsentState(enabled = true, allowed = listOf("marketing"), denied = listOf("advertising"))

        assertEquals(state.consentStamp, reservedValue.current())
    }

    @Test
    fun `given consent management disabled, when the reserved value is read, then nothing is asserted`() {
        stubConsentState(enabled = false, allowed = listOf("marketing"))

        assertNull(reservedValue.current())
    }

    @Test
    fun `given the same state, when the stamper runs, then it writes exactly the reserved value`() = runTest {
        stubConsentState(enabled = true, allowed = listOf("marketing"), denied = listOf("advertising"))
        val event = provideEvent()

        ConsentManagementPlugin().also { it.setup(mockAnalytics) }.intercept(event)

        assertEquals(reservedValue.current(), event.context[SDKManagedContextKey.CONSENT_MANAGEMENT.key])
    }

    @Test
    fun `given the consent reserved value, when its advice is read, then it points the caller at setConsent`() {
        assertTrue(reservedValue.overrideAdvice.contains("setConsent()"))
    }

    private fun stubConsentState(
        enabled: Boolean,
        allowed: List<String> = emptyList(),
        denied: List<String> = emptyList(),
    ): ConsentManagementState {
        val state = ConsentManagementState(
            enabled = enabled,
            provider = ConsentManagementProvider.CUSTOM,
            allowedConsentIds = allowed,
            deniedConsentIds = denied,
        )
        every { mockAnalytics.consentManagementState } returns State(initialState = state)
        return state
    }
}

private fun provideEvent(): Event = TrackEvent(event = EVENT_NAME, properties = emptyJsonObject)
