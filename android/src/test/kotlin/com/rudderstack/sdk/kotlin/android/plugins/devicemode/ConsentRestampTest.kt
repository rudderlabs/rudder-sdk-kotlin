package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockStandardIntegrationPlugin
import com.rudderstack.sdk.kotlin.android.utils.mergeWithHigherPriorityTo
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementProvider
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.consent.ConsentManagementState
import com.rudderstack.sdk.kotlin.core.internals.models.consent.toConsentContextBlock
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.StateAction
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

            plugin.intercept(trackEvent("injected-event").also { it.context = spoofedConsentPayload() })

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

            plugin.intercept(trackEvent("spoofed-event"))

            verify(exactly = 1) {
                plugin.track(match { it.context[CONSENT_MANAGEMENT_KEY] == expectedStamp(state) })
            }
        }

    @Test
    fun `given events buffered before source config, when replayed after a consent change, then they carry the stamp current at delivery`() =
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

            // Buffered while the destination awaits its source config, stamped with the initial state.
            managementPlugin.intercept(
                trackEvent("buffered-event").also {
                    it.context = it.context mergeWithHigherPriorityTo initialState.toConsentContextBlock()
                }
            )

            consentManagementState.dispatch(OverrideConsentStateAction(updatedState))
            sourceConfigState.dispatch(SourceConfig.UpdateAction(gatedSourceConfig()))
            testDispatcher.scheduler.advanceUntilIdle()

            verify(exactly = 1) {
                plugin.track(match { it.context[CONSENT_MANAGEMENT_KEY] == expectedStamp(updatedState) })
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

    private fun stubConsentState(state: ConsentManagementState) {
        every { mockAnalytics.consentManagementState } returns State(initialState = state)
    }

    private fun trackEvent(name: String): TrackEvent =
        TrackEvent(name, emptyJsonObject).also { applyBaseDataToEvent(it) }
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

// Replaces the consent state wholesale; the android module cannot reach the core-internal SetConsentAction.
private class OverrideConsentStateAction(
    private val newState: ConsentManagementState
) : StateAction<ConsentManagementState> {

    override fun reduce(currentState: ConsentManagementState): ConsentManagementState = newState
}

private fun consentState(allowed: List<String>) = ConsentManagementState(
    enabled = true,
    provider = ConsentManagementProvider.CUSTOM,
    allowedConsentIds = allowed,
    deniedConsentIds = emptyList(),
    initialized = true,
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
