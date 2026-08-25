package com.rudderstack.sdk.kotlin.core.plugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementProvider
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.consent.ConsentManagementState
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.skyscreamer.jsonassert.JSONAssert

private const val CONSENT_MANAGEMENT_KEY = "consentManagement"
private const val PROVIDER_KEY = "provider"
private const val ALLOWED_CONSENT_IDS_KEY = "allowedConsentIds"
private const val DENIED_CONSENT_IDS_KEY = "deniedConsentIds"

private const val EVENT_NAME = "Sample Event"
private const val SPOOFED_PROVIDER = "spoofed"
private const val SPOOFED_CONSENT_ID = "spoofed-id"
private const val CUSTOM_VALUE_SENTINEL = "sentinel-custom-value"
private const val NON_MANAGED_KEY = "campaign"

class ContextGuardPluginTest {

    @MockK
    private lateinit var mockAnalytics: Analytics

    private lateinit var snapshotPlugin: ContextSnapshotPlugin
    private lateinit var plugin: ContextGuardPlugin

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        snapshotPlugin = ContextSnapshotPlugin()
        every { mockAnalytics.contextSnapshotPlugin } returns snapshotPlugin
        plugin = ContextGuardPlugin()
        plugin.setup(mockAnalytics)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // Consent stamp enforcement

    @Test
    fun `given enabled state, when a plugin rewrote consentManagement, then the guard warns once and restores the stamp`() =
        runTest {
            stubConsentState(enabled = true, allowed = listOf("marketing"))
            val event = provideEvent().also { it.context = provideSpoofedConsentPayload() }

            plugin.intercept(event)

            JSONAssert.assertEquals(
                provideConsentContextPayload(allowed = listOf("marketing")).toString(),
                event.context.toString(),
                true
            )
            val messages = mutableListOf<String>()
            val mockLogger = mockAnalytics.logger
            verify(exactly = 1) { mockLogger.warn(capture(messages)) }
            assertTrue(messages.single().contains("setConsent()"))
        }

    @Test
    fun `given enabled state, when consentManagement is missing at the terminal boundary, then the guard warns and restamps`() =
        runTest {
            stubConsentState(enabled = true, allowed = listOf("marketing"))
            val event = provideEvent()

            plugin.intercept(event)

            JSONAssert.assertEquals(
                provideConsentContextPayload(allowed = listOf("marketing")).toString(),
                event.context.toString(),
                true
            )
            val mockLogger = mockAnalytics.logger
            verify(exactly = 1) { mockLogger.warn(any()) }
        }

    @Test
    fun `given a legacy injection already replaced by the early stamper, when the guard runs, then it stays silent`() =
        runTest {
            stubConsentState(enabled = true, allowed = listOf("marketing"))
            val stamper = ConsentManagementPlugin().also { it.setup(mockAnalytics) }
            val event = provideEvent().also { it.context = provideSpoofedConsentPayload() }

            stamper.intercept(event)
            plugin.intercept(event)

            val mockLogger = mockAnalytics.logger
            verify(exactly = 1) { mockLogger.warn(any()) }
        }

    @Test
    fun `given consent management disabled, when an event carries a customer consent block, then it passes through untouched`() =
        runTest {
            stubConsentState(enabled = false)
            val event = provideEvent().also { it.context = provideSpoofedConsentPayload() }

            plugin.intercept(event)

            JSONAssert.assertEquals(provideSpoofedConsentPayload().toString(), event.context.toString(), true)
            val mockLogger = mockAnalytics.logger
            verify(exactly = 0) { mockLogger.warn(any()) }
        }

    @Test
    fun `given the same state, when the guard restamps, then the block matches the early stamper output exactly`() =
        runTest {
            stubConsentState(enabled = true, allowed = listOf("marketing"), denied = listOf("advertising"))
            val stamperEvent = provideEvent()
            ConsentManagementPlugin().also { it.setup(mockAnalytics) }.intercept(stamperEvent)
            val guardEvent = provideEvent().also { it.context = provideSpoofedConsentPayload() }

            plugin.intercept(guardEvent)

            JSONAssert.assertEquals(stamperEvent.context.toString(), guardEvent.context.toString(), true)
        }

    // Base-key override detection

    @ParameterizedTest
    @ValueSource(strings = ["app", "device", "library", "locale", "network", "os", "screen", "timezone", "sessionId"])
    fun `given a base key injected via customContext, when the guard runs, then one warning names the key`(
        baseKey: String,
    ) = runTest {
        stubConsentState(enabled = false)
        val event = provideEvent(customContext = buildJsonObject { put(baseKey, CUSTOM_VALUE_SENTINEL) })

        plugin.intercept(event)

        val messages = mutableListOf<String>()
        val mockLogger = mockAnalytics.logger
        verify(exactly = 1) { mockLogger.warn(capture(messages)) }
        assertTrue(messages.single().contains("\"$baseKey\""))
    }

    @ParameterizedTest
    @ValueSource(strings = ["app", "device", "library", "locale", "network", "os", "screen", "timezone", "sessionId"])
    fun `given a plugin changed a base key after the snapshot, when the guard runs, then one warning names the key`(
        baseKey: String,
    ) = runTest {
        stubConsentState(enabled = false)
        val event = provideEvent().also { it.context = buildJsonObject { put(baseKey, "sdk-value") } }
        snapshotPlugin.intercept(event)
        event.context = buildJsonObject { put(baseKey, CUSTOM_VALUE_SENTINEL) }

        plugin.intercept(event)

        val messages = mutableListOf<String>()
        val mockLogger = mockAnalytics.logger
        verify(exactly = 1) { mockLogger.warn(capture(messages)) }
        assertTrue(messages.single().contains("\"$baseKey\""))
    }

    @Test
    fun `given the same key hit via customContext and the snapshot diff, when the guard runs, then it warns only once`() =
        runTest {
            stubConsentState(enabled = false)
            val event = provideEvent(customContext = buildJsonObject { put("library", CUSTOM_VALUE_SENTINEL) })
            event.context = buildJsonObject { put("library", "sdk-value") }
            snapshotPlugin.intercept(event)
            event.context = buildJsonObject { put("library", CUSTOM_VALUE_SENTINEL) }

            plugin.intercept(event)

            val mockLogger = mockAnalytics.logger
            verify(exactly = 1) { mockLogger.warn(any()) }
        }

    @Test
    fun `given a non-managed custom key injected and mutated, when the guard runs, then it stays silent`() = runTest {
        stubConsentState(enabled = false)
        val event = provideEvent(customContext = buildJsonObject { put(NON_MANAGED_KEY, CUSTOM_VALUE_SENTINEL) })
        event.context = buildJsonObject { put(NON_MANAGED_KEY, "initial") }
        snapshotPlugin.intercept(event)
        event.context = buildJsonObject { put(NON_MANAGED_KEY, "changed") }

        plugin.intercept(event)

        val mockLogger = mockAnalytics.logger
        verify(exactly = 0) { mockLogger.warn(any()) }
        assertEquals("\"changed\"", event.context[NON_MANAGED_KEY].toString())
    }

    @Test
    fun `given a base key override, when the guard warns, then the message never contains the custom value`() = runTest {
        stubConsentState(enabled = false)
        val event = provideEvent(customContext = buildJsonObject { put("library", CUSTOM_VALUE_SENTINEL) })

        plugin.intercept(event)

        val messages = mutableListOf<String>()
        val mockLogger = mockAnalytics.logger
        verify(exactly = 1) { mockLogger.warn(capture(messages)) }
        assertFalse(messages.single().contains(CUSTOM_VALUE_SENTINEL))
    }

    @Test
    fun `given a base key override detected, when the guard runs, then the delivered value is left untouched`() = runTest {
        stubConsentState(enabled = false)
        val event = provideEvent().also { it.context = buildJsonObject { put("library", "sdk-value") } }
        snapshotPlugin.intercept(event)
        event.context = buildJsonObject { put("library", CUSTOM_VALUE_SENTINEL) }

        plugin.intercept(event)

        assertEquals("\"$CUSTOM_VALUE_SENTINEL\"", event.context["library"].toString())
    }

    @Test
    fun `given a stale snapshot from another event, when the guard runs, then it stays silent and clears the slot`() =
        runTest {
            stubConsentState(enabled = false)
            val staleEvent = provideEvent().also { it.context = buildJsonObject { put("library", "sdk-value") } }
            snapshotPlugin.intercept(staleEvent)
            val event = provideEvent().also { it.context = buildJsonObject { put("library", CUSTOM_VALUE_SENTINEL) } }

            plugin.intercept(event)

            val mockLogger = mockAnalytics.logger
            verify(exactly = 0) { mockLogger.warn(any()) }
            assertEquals(null, snapshotPlugin.consumeSnapshot(staleEvent.messageId))
        }

    @Test
    fun `given a plugin rebuilt the context through serialization, when the guard runs, then it stays silent`() = runTest {
        stubConsentState(enabled = false)
        val event = provideEvent().also { it.context = provideMixedTypeContextPayload() }
        snapshotPlugin.intercept(event)
        event.context = Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(JsonObject.serializer(), event.context))

        plugin.intercept(event)

        val mockLogger = mockAnalytics.logger
        verify(exactly = 0) { mockLogger.warn(any()) }
    }

    // Helpers

    private fun stubConsentState(
        enabled: Boolean,
        allowed: List<String> = emptyList(),
        denied: List<String> = emptyList(),
    ) {
        every { mockAnalytics.consentManagementState } returns State(
            initialState = ConsentManagementState(
                enabled = enabled,
                provider = ConsentManagementProvider.CUSTOM,
                allowedConsentIds = allowed,
                deniedConsentIds = denied,
                initialized = enabled && (allowed.isNotEmpty() || denied.isNotEmpty()),
            )
        )
    }
}

private fun provideEvent(customContext: JsonObject = emptyJsonObject): Event = TrackEvent(
    event = EVENT_NAME,
    properties = emptyJsonObject,
    options = RudderOption(customContext = customContext),
)

private fun provideConsentContextPayload(
    allowed: List<String> = emptyList(),
    denied: List<String> = emptyList(),
): JsonObject = buildJsonObject {
    put(
        CONSENT_MANAGEMENT_KEY,
        buildJsonObject {
            put(PROVIDER_KEY, ConsentManagementProvider.CUSTOM.value)
            put(ALLOWED_CONSENT_IDS_KEY, buildJsonArray { allowed.forEach { add(it) } })
            put(DENIED_CONSENT_IDS_KEY, buildJsonArray { denied.forEach { add(it) } })
        }
    )
}

private fun provideSpoofedConsentPayload(): JsonObject = buildJsonObject {
    put(
        CONSENT_MANAGEMENT_KEY,
        buildJsonObject {
            put(PROVIDER_KEY, SPOOFED_PROVIDER)
            put(ALLOWED_CONSENT_IDS_KEY, buildJsonArray { add(SPOOFED_CONSENT_ID) })
        }
    )
}

private fun provideMixedTypeContextPayload(): JsonObject = buildJsonObject {
    put("app", buildJsonObject { put("name", "sample") })
    put("device", buildJsonObject { put("attTrackingStatus", 3) })
    put("network", buildJsonObject { put("wifi", true) })
    put("screen", buildJsonObject { put("density", 3) })
    put("timezone", "Asia/Kolkata")
    put("sessionId", 1724500000000L)
}
