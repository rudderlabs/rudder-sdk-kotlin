package com.rudderstack.sdk.kotlin.core.plugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementOptions
import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementProvider
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.consent.ConsentManagementState
import com.rudderstack.sdk.kotlin.core.internals.models.consent.SetConsentAction
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

private const val CONSENT_MANAGEMENT_KEY = "consentManagement"
private const val PROVIDER_KEY = "provider"
private const val ALLOWED_CONSENT_IDS_KEY = "allowedConsentIds"
private const val DENIED_CONSENT_IDS_KEY = "deniedConsentIds"

private const val EVENT_NAME = "Sample Event"
private const val LEGACY_PROVIDER = "legacy"
private const val LEGACY_CONSENT_ID = "legacy-id"

class ConsentManagementPluginTest {

    @MockK
    private lateinit var mockAnalytics: Analytics

    private lateinit var plugin: ConsentManagementPlugin

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        plugin = ConsentManagementPlugin()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given consent management enabled, when an event is intercepted, then the exact block is stamped`() = runTest {
        every { mockAnalytics.consentManagementState } returns provideConsentState(
            enabled = true,
            allowed = listOf("marketing"),
            denied = listOf("advertising"),
        )
        val event = provideEvent()

        plugin.setup(mockAnalytics)
        plugin.intercept(event)

        JSONAssert.assertEquals(
            provideConsentContextPayload(allowed = listOf("marketing"), denied = listOf("advertising")).toString(),
            event.context.toString(),
            true
        )
    }

    @Test
    fun `given consent management disabled, when an event is intercepted, then the consentManagement key is absent`() = runTest {
        every { mockAnalytics.consentManagementState } returns provideConsentState(enabled = false)
        val event = provideEvent()

        plugin.setup(mockAnalytics)
        plugin.intercept(event)

        assertFalse(event.context.containsKey(CONSENT_MANAGEMENT_KEY))
        assertEquals(emptyJsonObject, event.context)
    }

    @Test
    fun `given a legacy injected key while enabled, when an event is intercepted, then the sdk block wins and a warning is logged`() = runTest {
        every { mockAnalytics.consentManagementState } returns provideConsentState(
            enabled = true,
            allowed = listOf("marketing"),
        )
        val mockLogger = mockAnalytics.logger
        val event = provideEventWithLegacyKey()

        plugin.setup(mockAnalytics)
        plugin.intercept(event)

        JSONAssert.assertEquals(
            provideConsentContextPayload(allowed = listOf("marketing"), denied = emptyList()).toString(),
            event.context.toString(),
            true
        )
        val messages = mutableListOf<String>()
        verify(exactly = 1) { mockLogger.warn(capture(messages)) }
        assertTrue(messages.single().contains(CONSENT_MANAGEMENT_KEY))
        assertFalse(messages.single().contains(LEGACY_CONSENT_ID))
    }

    @Test
    fun `given a legacy injected key while disabled, when an event is intercepted, then the key is preserved with no warning`() = runTest {
        every { mockAnalytics.consentManagementState } returns provideConsentState(enabled = false)
        val mockLogger = mockAnalytics.logger
        val event = provideEventWithLegacyKey()

        plugin.setup(mockAnalytics)
        plugin.intercept(event)

        JSONAssert.assertEquals(provideLegacyContextPayload().toString(), event.context.toString(), true)
        verify(exactly = 0) { mockLogger.warn(any()) }
    }

    @Test
    fun `given the state is updated between events, when a second event is intercepted, then it carries the new lists`() = runTest {
        val consentState = provideConsentState(enabled = true, allowed = listOf("marketing"))
        every { mockAnalytics.consentManagementState } returns consentState
        plugin.setup(mockAnalytics)

        val firstEvent = provideEvent()
        plugin.intercept(firstEvent)

        consentState.dispatch(
            SetConsentAction(
                ConsentManagementOptions(
                    allowedConsentIds = listOf("analytics"),
                    deniedConsentIds = listOf("advertising"),
                )
            )
        )

        val secondEvent = provideEvent()
        plugin.intercept(secondEvent)

        JSONAssert.assertEquals(
            provideConsentContextPayload(allowed = listOf("marketing"), denied = emptyList()).toString(),
            firstEvent.context.toString(),
            true
        )
        JSONAssert.assertEquals(
            provideConsentContextPayload(allowed = listOf("analytics"), denied = listOf("advertising")).toString(),
            secondEvent.context.toString(),
            true
        )
    }
}

private fun provideEvent(): Event = TrackEvent(
    event = EVENT_NAME,
    properties = emptyJsonObject,
)

private fun provideEventWithLegacyKey(): Event = provideEvent().also {
    it.context = provideLegacyContextPayload()
}

private fun provideConsentState(
    enabled: Boolean,
    allowed: List<String> = emptyList(),
    denied: List<String> = emptyList(),
): State<ConsentManagementState> = State(
    initialState = ConsentManagementState(
        enabled = enabled,
        provider = ConsentManagementProvider.CUSTOM,
        allowedConsentIds = allowed,
        deniedConsentIds = denied,
        initialized = enabled && (allowed.isNotEmpty() || denied.isNotEmpty()),
    )
)

private fun provideConsentContextPayload(allowed: List<String>, denied: List<String>): JsonObject = buildJsonObject {
    put(
        CONSENT_MANAGEMENT_KEY,
        buildJsonObject {
            put(PROVIDER_KEY, ConsentManagementProvider.CUSTOM.value)
            put(ALLOWED_CONSENT_IDS_KEY, buildJsonArray { allowed.forEach { add(it) } })
            put(DENIED_CONSENT_IDS_KEY, buildJsonArray { denied.forEach { add(it) } })
        }
    )
}

private fun provideLegacyContextPayload(): JsonObject = buildJsonObject {
    put(
        CONSENT_MANAGEMENT_KEY,
        buildJsonObject {
            put(PROVIDER_KEY, LEGACY_PROVIDER)
            put(ALLOWED_CONSENT_IDS_KEY, buildJsonArray { add(LEGACY_CONSENT_ID) })
        }
    )
}
