package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockCustomIntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockStandardIntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.ReplaceConsentStateAction
import com.rudderstack.sdk.kotlin.android.utils.mergeWithHigherPriorityTo
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.android.consent.ConsentManagementProvider
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.android.models.consent.ConsentManagementState
import com.rudderstack.sdk.kotlin.android.models.consent.consentStamp
import com.rudderstack.sdk.kotlin.android.models.consent.toConsentContextBlock
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val CONSENT_MANAGEMENT_KEY = "consentManagement"
private const val SPOOFED_PROVIDER = "spoofed"
private const val SPOOFED_CONSENT_ID = "spoofed-id"

@OptIn(ExperimentalCoroutinesApi::class)
class ConsentRestampTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    private lateinit var plugin: MockStandardIntegrationPlugin

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { mockAnalytics.configuration } returns mockk<Configuration>(relaxed = true)
        every { mockAnalytics.sourceConfigState } returns State(initialState = SourceConfig.initialState())
        plugin = spyk(MockStandardIntegrationPlugin())
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `given an event carrying an injected consent block, when delivered, then the destination receives the state stamp`() =
        runTest(testDispatcher) {
            val state = consentState(allowed = listOf("marketing"))
            stubConsentState(state)
            plugin.setup(mockAnalytics)
            plugin.initDestination(gatedSourceConfig())

            plugin.intercept(
                trackEvent("injected-event", capturedUnder = state).also { it.context = spoofedConsentPayload() }
            )

            verify(exactly = 1) {
                plugin.track(match { it.context[CONSENT_MANAGEMENT_KEY] == expectedStamp(state) })
            }
        }

    @Test
    fun `given a destination plugin spoofing the consent block, when delivered, then the handoff carries the state stamp`() =
        runTest(testDispatcher) {
            val state = consentState(allowed = listOf("marketing"))
            stubConsentState(state)
            plugin.setup(mockAnalytics)
            plugin.initDestination(gatedSourceConfig())
            plugin.add(SpoofConsentPlugin())

            plugin.intercept(trackEvent("spoofed-event", capturedUnder = state))

            verify(exactly = 1) {
                plugin.track(match { it.context[CONSENT_MANAGEMENT_KEY] == expectedStamp(state) })
            }
        }

    // The stamp records the decision the event was created under. Replaying it later, under a
    // newer decision, must not rewrite what it says the user had agreed to at the time.
    @Test
    fun `given events buffered before source config, when replayed after a consent change, then they carry the stamp captured at creation`() =
        runTest(testDispatcher) {
            val initialState = consentState(allowed = listOf("marketing", "analytics"))
            val updatedState = consentState(allowed = listOf("marketing"))
            val consentManagementState = State(initialState = initialState)
            every { mockAnalytics.consentManagementState } returns consentManagementState
            val sourceConfigState = State(initialState = SourceConfig.initialState())
            every { mockAnalytics.sourceConfigState } returns sourceConfigState

            val managementPlugin = IntegrationsManagementPlugin()
            managementPlugin.setup(mockAnalytics)
            plugin.setup(mockAnalytics)
            managementPlugin.addIntegration(plugin)

            // Buffered while the destination awaits its source config, carrying the decision that
            // was in force when it was created.
            managementPlugin.intercept(
                trackEvent("buffered-event", capturedUnder = initialState).also {
                    it.context = it.context mergeWithHigherPriorityTo initialState.toConsentContextBlock()
                }
            )

            consentManagementState.dispatch(ReplaceConsentStateAction(updatedState))
            sourceConfigState.dispatch(SourceConfig.UpdateAction(gatedSourceConfig()))
            testDispatcher.scheduler.advanceUntilIdle()

            verify(exactly = 1) {
                plugin.track(match { it.context[CONSENT_MANAGEMENT_KEY] == expectedStamp(initialState) })
            }
        }

    @Test
    fun `given consent management disabled, when an event with a customer consent block is delivered, then it is untouched`() =
        runTest(testDispatcher) {
            stubConsentState(ConsentManagementState())
            plugin.setup(mockAnalytics)
            plugin.initDestination(gatedSourceConfig())

            plugin.intercept(trackEvent("legacy-event").also { it.context = spoofedConsentPayload() })

            verify(exactly = 1) {
                plugin.track(match { it.context[CONSENT_MANAGEMENT_KEY] == spoofedConsentPayload()[CONSENT_MANAGEMENT_KEY] })
            }
        }

    @Test
    fun `given a consent flip dropping the event at the gate, when intercepted, then nothing is delivered`() =
        runTest(testDispatcher) {
            stubConsentState(consentState(allowed = listOf("marketing")))
            val sourceConfigState = State(initialState = SourceConfig.initialState())
            every { mockAnalytics.sourceConfigState } returns sourceConfigState
            plugin.setup(mockAnalytics)
            sourceConfigState.dispatch(SourceConfig.UpdateAction(gatedSourceConfig()))
            testDispatcher.scheduler.advanceUntilIdle()
            plugin.initDestination(gatedSourceConfig())

            stubConsentState(consentState(allowed = listOf("something-else")))
            plugin.intercept(trackEvent("gated-event"))

            verify(exactly = 0) { plugin.track(any()) }
        }

    @Test
    fun `given consent revoked while an event is mid chain, when it resumes, then it is not delivered`() =
        runTest(testDispatcher) {
            val consentManagementState = State(initialState = consentState(allowed = listOf("marketing")))
            every { mockAnalytics.consentManagementState } returns consentManagementState
            val sourceConfigState = State(initialState = SourceConfig.initialState())
            every { mockAnalytics.sourceConfigState } returns sourceConfigState
            plugin.setup(mockAnalytics)
            sourceConfigState.dispatch(SourceConfig.UpdateAction(gatedSourceConfig()))
            testDispatcher.scheduler.advanceUntilIdle()
            plugin.initDestination(gatedSourceConfig())

            // A customer plugin doing suspending work parks the event inside the destination chain,
            // after the consent gate has already passed it.
            val released = CompletableDeferred<Unit>()
            plugin.add(ParkingPlugin(released))
            launch { plugin.intercept(trackEvent("in-flight-event")) }
            testDispatcher.scheduler.advanceUntilIdle()

            consentManagementState.dispatch(ReplaceConsentStateAction(consentState(allowed = listOf("something-else"))))
            released.complete(Unit)
            testDispatcher.scheduler.advanceUntilIdle()

            verify(exactly = 0) { plugin.track(any()) }
        }

    // A custom integration skips the source config at creation, but ConsentGatePlugin still resolves
    // its consent rules from the destination matching its key. The handoff gate has to apply the same
    // rules, or a revocation landing while the event is parked is honoured at entry and ignored here.
    @Test
    fun `given a custom integration with consent rules, when consent is revoked while an event is mid chain, then it is not delivered`() =
        runTest(testDispatcher) {
            val consentManagementState = State(initialState = consentState(allowed = listOf("marketing")))
            every { mockAnalytics.consentManagementState } returns consentManagementState
            val sourceConfigState = State(initialState = SourceConfig.initialState())
            every { mockAnalytics.sourceConfigState } returns sourceConfigState
            val customPlugin = spyk(MockCustomIntegrationPlugin())
            customPlugin.setup(mockAnalytics)
            sourceConfigState.dispatch(SourceConfig.UpdateAction(gatedSourceConfig()))
            testDispatcher.scheduler.advanceUntilIdle()
            customPlugin.initDestination(gatedSourceConfig())
            assertTrue(customPlugin.isDestinationReady, "Precondition: the custom integration is ready.")

            val released = CompletableDeferred<Unit>()
            customPlugin.add(ParkingPlugin(released))
            launch { customPlugin.intercept(trackEvent("in-flight-event")) }
            testDispatcher.scheduler.advanceUntilIdle()

            consentManagementState.dispatch(ReplaceConsentStateAction(consentState(allowed = listOf("something-else"))))
            released.complete(Unit)
            testDispatcher.scheduler.advanceUntilIdle()

            verify(exactly = 0) { customPlugin.track(any()) }
        }

    @Test
    fun `given the source config tightens consent while an event is mid chain, when it resumes, then it is not delivered`() =
        runTest(testDispatcher) {
            val consentManagementState = State(initialState = consentState(allowed = listOf("marketing")))
            every { mockAnalytics.consentManagementState } returns consentManagementState
            val sourceConfigState = State(initialState = SourceConfig.initialState())
            every { mockAnalytics.sourceConfigState } returns sourceConfigState
            plugin.setup(mockAnalytics)
            sourceConfigState.dispatch(SourceConfig.UpdateAction(gatedSourceConfig()))
            testDispatcher.scheduler.advanceUntilIdle()
            plugin.initDestination(gatedSourceConfig())

            val released = CompletableDeferred<Unit>()
            plugin.add(ParkingPlugin(released))
            launch { plugin.intercept(trackEvent("in-flight-event")) }
            testDispatcher.scheduler.advanceUntilIdle()

            // The dashboard now demands a second consent the user has never granted. The update is
            // rejected, so the destination is no longer ready - but the parked event already passed
            // that check and must be judged against the new rules, not the ones it started under.
            plugin.initDestination(gatedSourceConfig(consents = listOf("marketing", "analytics")))
            released.complete(Unit)
            testDispatcher.scheduler.advanceUntilIdle()

            verify(exactly = 0) { plugin.track(any()) }
        }

    // The main-chain guard never sees a write made inside a destination's own chain, so this is the
    // only place the customer hears about it. A plugin that spoofs does so on every event, so the
    // report is raised once per destination rather than once per event.
    @Test
    fun `given a destination plugin spoofing on every event, when several are delivered, then only the first is reported`() =
        runTest(testDispatcher) {
            val state = consentState(allowed = listOf("marketing"))
            stubConsentState(state)
            plugin.setup(mockAnalytics)
            plugin.initDestination(gatedSourceConfig())
            plugin.add(SpoofConsentPlugin())

            repeat(times = 3) { plugin.intercept(trackEvent("spoofed-event", capturedUnder = state)) }

            val messages = mutableListOf<String>()
            verify(atLeast = 0) { mockAnalytics.logger.warn(capture(messages)) }
            assertEquals(1, messages.count { it.contains(CONSENT_MANAGEMENT_KEY) }, "warnings: $messages")
        }

    private fun stubConsentState(state: ConsentManagementState) {
        every { mockAnalytics.consentManagementState } returns State(initialState = state)
    }

    private fun trackEvent(name: String, capturedUnder: ConsentManagementState? = null): TrackEvent =
        TrackEvent(name, emptyJsonObject).also {
            applyBaseDataToEvent(it)
            if (capturedUnder != null) {
                it.capturedReservedContext = mapOf(CONSENT_MANAGEMENT_KEY to capturedUnder.consentStamp)
            }
        }
}

// Customer-style destination plugin overwriting the consent block after the main-chain stamp.
private class SpoofConsentPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.OnProcess

    override lateinit var analytics: Analytics

    override suspend fun intercept(event: Event): Event {
        event.context = event.context mergeWithHigherPriorityTo spoofedConsentPayload()
        return event
    }
}

// A customer plugin doing real suspending work, holding the event inside the destination chain.
private class ParkingPlugin(private val released: CompletableDeferred<Unit>) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.OnProcess

    override lateinit var analytics: Analytics

    override suspend fun intercept(event: Event): Event {
        released.await()
        return event
    }
}

private fun consentState(allowed: List<String>) = ConsentManagementState(
    active = true,
    provider = ConsentManagementProvider.CUSTOM,
    allowedConsentIds = allowed,
    deniedConsentIds = emptyList(),
)

private fun expectedStamp(state: ConsentManagementState) = state.toConsentContextBlock()[CONSENT_MANAGEMENT_KEY]

private fun gatedSourceConfig(consents: List<String> = listOf("marketing"), strategy: String = "and"): SourceConfig {
    val consentObjects = consents.joinToString(",") { """{ "consent": "$it" }""" }
    return LenientJson.decodeFromString(
        """
        {
          "source": {
            "id": "<SOURCE_ID>",
            "name": "Android",
            "writeKey": "<WRITE_KEY>",
            "enabled": true,
            "workspaceId": "<WORKSPACE_ID>",
            "updatedAt": "2024-08-28T12:53:34.870Z",
            "destinations": [
              {
                "id": "<DESTINATION_ID>",
                "name": "Mock Destination",
                "enabled": true,
                "config": {
                  "apiKey": "test-api-key",
                  "consentManagement": [
                    { "provider": "custom", "consents": [ $consentObjects ], "resolutionStrategy": "$strategy" }
                  ]
                },
                "destinationDefinitionId": "<DESTINATION_DEFINITION_ID>",
                "destinationDefinition": { "name": "MOCK DESTINATION", "displayName": "MockDestination" },
                "updatedAt": "2024-08-28T12:53:34.870Z",
                "shouldApplyDeviceModeTransformation": false,
                "propagateEventsUntransformedOnError": false
              }
            ]
          }
        }
        """
    )
}

private fun spoofedConsentPayload(): JsonObject = buildJsonObject {
    put(
        CONSENT_MANAGEMENT_KEY,
        buildJsonObject {
            put("provider", SPOOFED_PROVIDER)
            put("allowedConsentIds", buildJsonArray { add(SPOOFED_CONSENT_ID) })
        }
    )
}
