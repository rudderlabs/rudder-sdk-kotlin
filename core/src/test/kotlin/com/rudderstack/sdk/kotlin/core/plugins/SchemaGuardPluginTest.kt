package com.rudderstack.sdk.kotlin.core.plugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.ReservedContextValue
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.SDKManagedContextKey
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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

private const val EVENT_NAME = "Sample Event"
private const val CUSTOM_VALUE_SENTINEL = "sentinel-custom-value"
private const val NON_MANAGED_KEY = "campaign"
private const val STUB_ADVICE = "stub advice for a reserved key."
private val RESERVED_KEY = SDKManagedContextKey.CONSENT_MANAGEMENT
private val STUB_VALUE = JsonPrimitive("sdk-owned-value")
private val CUSTOMER_VALUE = JsonPrimitive("customer-value")

class SchemaGuardPluginTest {

    @MockK
    private lateinit var mockAnalytics: Analytics

    private lateinit var snapshotPlugin: ContextSnapshotPlugin
    private lateinit var plugin: SchemaGuardPlugin
    private val registry = mutableMapOf<SDKManagedContextKey, ReservedContextValue>()

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        snapshotPlugin = ContextSnapshotPlugin()
        every { mockAnalytics.contextSnapshotPlugin } returns snapshotPlugin
        every { mockAnalytics.getPlatformType() } returns PlatformType.Mobile
        // Empty by default: nothing is reserved unless a test registers a supplier.
        every { mockAnalytics.reservedContextValues } returns registry
        plugin = SchemaGuardPlugin()
        plugin.setup(mockAnalytics)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // Reserved-key re-assertion
    //
    // The registered value is deliberately not a consent block: the guard is driven by the
    // registry, so it must re-assert whatever it is handed, for any reserved key.

    @Test
    fun `given a registered reserved value, when a plugin rewrote the key, then the guard warns once and restores it`() =
        runTest {
            registry[RESERVED_KEY] = StubReservedValue(STUB_VALUE)
            val event = provideEvent().also {
                it.context = buildJsonObject { put(RESERVED_KEY.key, CUSTOMER_VALUE) }
            }

            plugin.intercept(event)

            assertEquals(STUB_VALUE, event.context[RESERVED_KEY.key])
            val mockLogger = mockAnalytics.logger
            verify(exactly = 1) { mockLogger.warn(any()) }
        }

    @Test
    fun `given a registered reserved value, when the key is absent, then the guard stamps it`() = runTest {
        registry[RESERVED_KEY] = StubReservedValue(STUB_VALUE)
        val event = provideEvent()

        plugin.intercept(event)

        assertEquals(STUB_VALUE, event.context[RESERVED_KEY.key])
    }

    @Test
    fun `given a registered value the event already carries, when the guard runs, then it stays silent`() = runTest {
        registry[RESERVED_KEY] = StubReservedValue(STUB_VALUE)
        val event = provideEvent().also {
            it.context = buildJsonObject { put(RESERVED_KEY.key, STUB_VALUE) }
        }

        plugin.intercept(event)

        assertEquals(STUB_VALUE, event.context[RESERVED_KEY.key])
        val mockLogger = mockAnalytics.logger
        verify(exactly = 0) { mockLogger.warn(any()) }
    }

    @Test
    fun `given a supplier asserting no value, when the guard runs, then the event passes through untouched`() =
        runTest {
            registry[RESERVED_KEY] = StubReservedValue(null)
            val event = provideEvent().also {
                it.context = buildJsonObject { put(RESERVED_KEY.key, CUSTOMER_VALUE) }
            }

            plugin.intercept(event)

            assertEquals(CUSTOMER_VALUE, event.context[RESERVED_KEY.key])
            val mockLogger = mockAnalytics.logger
            verify(exactly = 0) { mockLogger.warn(any()) }
        }

    @Test
    fun `given no registered supplier, when the guard runs, then the key is not reserved`() = runTest {
        val event = provideEvent().also {
            it.context = buildJsonObject { put(RESERVED_KEY.key, CUSTOMER_VALUE) }
        }

        plugin.intercept(event)

        assertEquals(CUSTOMER_VALUE, event.context[RESERVED_KEY.key])
        val mockLogger = mockAnalytics.logger
        verify(exactly = 0) { mockLogger.warn(any()) }
    }

    @Test
    fun `given a registered supplier, when the guard warns, then the message carries that supplier's advice`() =
        runTest {
            registry[RESERVED_KEY] = StubReservedValue(STUB_VALUE)
            val event = provideEvent().also {
                it.context = buildJsonObject { put(RESERVED_KEY.key, CUSTOMER_VALUE) }
            }

            plugin.intercept(event)

            val messages = mutableListOf<String>()
            val mockLogger = mockAnalytics.logger
            verify(exactly = 1) { mockLogger.warn(capture(messages)) }
            assertTrue(messages.single().contains(STUB_ADVICE))
        }

    // Base-key override detection

    @ParameterizedTest
    @ValueSource(strings = ["app", "device", "locale", "network", "os", "screen", "timezone", "sessionId"])
    fun `given a server platform, when a mobile base key is injected via customContext, then no warning is logged`(
        baseKey: String,
    ) = runTest {
        every { mockAnalytics.getPlatformType() } returns PlatformType.Server
        val event = provideEvent(customContext = buildJsonObject { put(baseKey, CUSTOM_VALUE_SENTINEL) })

        plugin.intercept(event)

        verify(exactly = 0) { mockAnalytics.logger.warn(any()) }
    }

    @Test
    fun `given a server platform, when library is injected via customContext, then it still warns`() =
        runTest {
            every { mockAnalytics.getPlatformType() } returns PlatformType.Server
            val event = provideEvent(customContext = buildJsonObject { put("library", CUSTOM_VALUE_SENTINEL) })

            plugin.intercept(event)

            val messages = mutableListOf<String>()
            verify(exactly = 1) { mockAnalytics.logger.warn(capture(messages)) }
            assertTrue(messages.single().contains("\"library\""))
        }

    @ParameterizedTest
    @ValueSource(strings = ["app", "device", "library", "locale", "network", "os", "screen", "timezone", "sessionId"])
    fun `given a base key injected via customContext, when the guard runs, then one warning names the key`(
        baseKey: String,
    ) = runTest {
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
        val event = provideEvent(customContext = buildJsonObject { put("library", CUSTOM_VALUE_SENTINEL) })

        plugin.intercept(event)

        val messages = mutableListOf<String>()
        val mockLogger = mockAnalytics.logger
        verify(exactly = 1) { mockLogger.warn(capture(messages)) }
        assertFalse(messages.single().contains(CUSTOM_VALUE_SENTINEL))
    }

    @Test
    fun `given a base key override detected, when the guard runs, then the delivered value is left untouched`() = runTest {
        val event = provideEvent().also { it.context = buildJsonObject { put("library", "sdk-value") } }
        snapshotPlugin.intercept(event)
        event.context = buildJsonObject { put("library", CUSTOM_VALUE_SENTINEL) }

        plugin.intercept(event)

        assertEquals("\"$CUSTOM_VALUE_SENTINEL\"", event.context["library"].toString())
    }

    @Test
    fun `given a stale snapshot from another event, when the guard runs, then it stays silent and clears the slot`() =
        runTest {
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
        val event = provideEvent().also { it.context = provideMixedTypeContextPayload() }
        snapshotPlugin.intercept(event)
        event.context = Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(JsonObject.serializer(), event.context))

        plugin.intercept(event)

        val mockLogger = mockAnalytics.logger
        verify(exactly = 0) { mockLogger.warn(any()) }
    }

    // Helpers

}

private fun provideEvent(customContext: JsonObject = emptyJsonObject): Event = TrackEvent(
    event = EVENT_NAME,
    properties = emptyJsonObject,
    options = RudderOption(customContext = customContext),
)

private fun provideMixedTypeContextPayload(): JsonObject = buildJsonObject {
    put("app", buildJsonObject { put("name", "sample") })
    put("device", buildJsonObject { put("attTrackingStatus", 3) })
    put("network", buildJsonObject { put("wifi", true) })
    put("screen", buildJsonObject { put("density", 3) })
    put("timezone", "Asia/Kolkata")
    put("sessionId", 1724500000000L)
}

private class StubReservedValue(
    private val value: JsonElement?,
    override val overrideAdvice: String = STUB_ADVICE,
) : ReservedContextValue {

    override fun current(): JsonElement? = value
}
