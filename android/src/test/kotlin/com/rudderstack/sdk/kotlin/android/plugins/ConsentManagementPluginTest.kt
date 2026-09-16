package com.rudderstack.sdk.kotlin.android.plugins

import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.consent.ConsentManagementConfiguration
import com.rudderstack.sdk.kotlin.android.consent.ConsentManagementProvider
import com.rudderstack.sdk.kotlin.android.models.consent.ConsentManagementState
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SDKManagedContextKey
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        // Reading the consent state touches AnalyticsUtils, which captures Dispatchers.Main in a
        // file-level val the first time it loads. Without a Main dispatcher installed, that capture
        // poisons every later test in the JVM that dispatches to the main thread.
        Dispatchers.setMain(testDispatcher)
        plugin = ConsentManagementPlugin()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `given consent management enabled, when an event is intercepted, then the exact block is stamped`() = runTest {
        val event = provideEventCapturedUnder(allowed = listOf("marketing"), denied = listOf("advertising"))

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
        // "Disabled" is represented by the absence of a captured value: the reserved-value supplier
        // asserts nothing while consent management is inactive, so nothing is captured at creation.
        val event = provideEvent()

        plugin.setup(mockAnalytics)
        plugin.intercept(event)

        assertFalse(event.context.containsKey(CONSENT_MANAGEMENT_KEY))
        assertEquals(emptyJsonObject, event.context)
    }

    @Test
    fun `given consent management enabled with no consent ids, when an event is intercepted, then the consentManagement key is absent`() = runTest {
        // Built through the real factory, not a hand-made inactive state: enabling consent management
        // without supplying either list is a configuration error, and an inactive session captures
        // nothing for the plugin to stamp.
        val state = ConsentManagementState.initialState(ConsentManagementConfiguration(enabled = true))
        every { mockAnalytics.consentManagementState } returns State(initialState = state)
        val event = provideEvent()

        plugin.setup(mockAnalytics)
        plugin.intercept(event)

        assertFalse(state.active, "Enabling with neither list must leave the session inactive.")
        assertFalse(event.context.containsKey(CONSENT_MANAGEMENT_KEY))
        assertEquals(emptyJsonObject, event.context)
    }

    @Test
    fun `given a legacy injected key while enabled, when an event is intercepted, then the sdk block wins and a warning is logged`() = runTest {
        val mockLogger = mockAnalytics.logger
        val event = provideEventWithLegacyKey(capturedAllowed = listOf("marketing"))

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
    fun `given the key is injected on every event, when several are intercepted, then only the first warns`() = runTest {
        val mockLogger = mockAnalytics.logger
        plugin.setup(mockAnalytics)

        repeat(times = 3) { plugin.intercept(provideEventWithLegacyKey(capturedAllowed = listOf("marketing"))) }

        verify(exactly = 1) { mockLogger.warn(any()) }
    }

    @Test
    fun `given a legacy injected key while disabled, when an event is intercepted, then the key is preserved with no warning`() = runTest {
        val mockLogger = mockAnalytics.logger
        val event = provideEventWithLegacyKey()

        plugin.setup(mockAnalytics)
        plugin.intercept(event)

        JSONAssert.assertEquals(provideLegacyContextPayload().toString(), event.context.toString(), true)
        verify(exactly = 0) { mockLogger.warn(any()) }
    }

    // Capture happens at creation, so a consent change between two events shows up as the two
    // events carrying different captured values — not as one live value applied to both.
    @Test
    fun `given consent changed between two events, when each is intercepted, then each carries the value captured at its own creation`() = runTest {
        plugin.setup(mockAnalytics)

        val firstEvent = provideEventCapturedUnder(allowed = listOf("marketing"))
        plugin.intercept(firstEvent)

        val secondEvent = provideEventCapturedUnder(
            allowed = listOf("analytics"),
            denied = listOf("advertising"),
        )
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

    // The stamp records the decision the event was created under. A later decision belongs to
    // later events, so it must not reach back and rewrite this one.
    @Test
    fun `given an event captured under an earlier consent value, when the state has since changed, then the plugin stamps the captured value`() =
        runTest {
            every { mockAnalytics.consentManagementState } returns provideConsentState(
                active = true,
                allowed = listOf("analytics"),
            )
            val event = provideEventCapturedUnder(allowed = listOf("marketing"))

            plugin.setup(mockAnalytics)
            plugin.intercept(event)

            JSONAssert.assertEquals(
                provideConsentContextPayload(allowed = listOf("marketing"), denied = emptyList()).toString(),
                event.context.toString(),
                true
            )
        }
}

private fun provideEvent(): Event = TrackEvent(
    event = EVENT_NAME,
    properties = emptyJsonObject,
)

private fun provideEventWithLegacyKey(capturedAllowed: List<String>? = null): Event = when (capturedAllowed) {
    null -> provideEvent()
    else -> provideEventCapturedUnder(allowed = capturedAllowed)
}.also {
    it.context = provideLegacyContextPayload()
}

private fun provideConsentState(
    active: Boolean,
    allowed: List<String> = emptyList(),
    denied: List<String> = emptyList(),
): State<ConsentManagementState> = State(
    initialState = ConsentManagementState(
        active = active,
        provider = ConsentManagementProvider.CUSTOM,
        allowedConsentIds = allowed,
        deniedConsentIds = denied,
    )
)

/** An event carrying the consent block the SDK asserted when it was created. */
private fun provideEventCapturedUnder(allowed: List<String>, denied: List<String> = emptyList()): Event =
    provideEvent().also {
        it.capturedReservedContext = mapOf(
            SDKManagedContextKey.CONSENT_MANAGEMENT.key to provideConsentStamp(allowed, denied)
        )
    }

/** The inner block — what a reserved-value supplier hands over, unwrapped. */
private fun provideConsentStamp(allowed: List<String>, denied: List<String>): JsonObject = buildJsonObject {
    put(PROVIDER_KEY, ConsentManagementProvider.CUSTOM.value)
    put(ALLOWED_CONSENT_IDS_KEY, buildJsonArray { allowed.forEach { add(it) } })
    put(DENIED_CONSENT_IDS_KEY, buildJsonArray { denied.forEach { add(it) } })
}

private fun provideConsentContextPayload(allowed: List<String>, denied: List<String>): JsonObject = buildJsonObject {
    put(CONSENT_MANAGEMENT_KEY, provideConsentStamp(allowed, denied))
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
