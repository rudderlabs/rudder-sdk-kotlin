package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockStandardIntegrationPlugin
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementProvider
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.consent.ConsentManagementState
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.StateAction
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConsentGatingTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)
    private val mockLogger = mockAnalytics.logger

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

    // Initialization gate

    @Test
    fun `given a denied destination, when initialized, then create is skipped with a consent denied callback`() =
        runTest(testDispatcher) {
            stubConsentState(consentState(allowed = listOf("something-else")))
            plugin.setup(mockAnalytics)
            var receivedResult: DestinationResult? = null
            plugin.onDestinationReady { _, result -> receivedResult = result }

            plugin.initDestination(gatedSourceConfig())

            assertNull(plugin.getDestinationInstance())
            assertFalse(plugin.isDestinationReady)
            assertTrue((receivedResult as? Result.Failure)?.error is ConsentDeniedException)
            verify {
                mockLogger.warn(
                    match { it.contains("denied by user consent") && !it.contains("marketing") }
                )
            }
        }

    @Test
    fun `given a denied destination whose update throws, when initialized, then the callback still reports consent denial`() =
        runTest(testDispatcher) {
            stubConsentState(consentState(allowed = listOf("something-else")))
            plugin.setup(mockAnalytics)
            every { plugin.update(any()) } throws IllegalArgumentException("Field 'appID' is required")
            var receivedResult: DestinationResult? = null
            plugin.onDestinationReady { _, result -> receivedResult = result }

            plugin.initDestination(gatedSourceConfig())

            assertFalse(plugin.isDestinationReady)
            assertTrue((receivedResult as? Result.Failure)?.error is ConsentDeniedException)
        }

    @Test
    fun `given a denied destination, when initialized, then the destination is never updated`() =
        runTest(testDispatcher) {
            stubConsentState(consentState(allowed = listOf("something-else")))
            plugin.setup(mockAnalytics)

            plugin.initDestination(gatedSourceConfig())

            verify(exactly = 0) { plugin.update(any()) }
        }

    @Test
    fun `given a consent denied callback that throws, when initialized, then the exception does not escape`() =
        runTest(testDispatcher) {
            stubConsentState(consentState(allowed = listOf("something-else")))
            plugin.setup(mockAnalytics)
            plugin.onDestinationReady { _, _ -> throw IllegalStateException("callback blew up") }

            plugin.initDestination(gatedSourceConfig())

            assertFalse(plugin.isDestinationReady)
        }

    @Test
    fun `given consent management disabled, when a gated destination is initialized, then behavior is unchanged`() =
        runTest(testDispatcher) {
            stubConsentState(ConsentManagementState())
            plugin.setup(mockAnalytics)

            plugin.initDestination(gatedSourceConfig())
            plugin.intercept(trackEvent("regular-event"))

            assertTrue(plugin.isDestinationReady)
            verify(exactly = 1) { plugin.track(match { it.event == "regular-event" }) }
        }

    // Grant mid-session

    @Test
    fun `given a grant after denial, when reinitialized, then the destination is late created`() =
        runTest(testDispatcher) {
            stubConsentState(consentState(allowed = listOf("something-else")))
            plugin.setup(mockAnalytics)
            plugin.initDestination(gatedSourceConfig())
            assertFalse(plugin.isDestinationReady)

            stubConsentState(consentState(allowed = listOf("marketing")))
            plugin.initDestination(gatedSourceConfig())

            assertTrue(plugin.isDestinationReady)
        }

    @Test
    fun `given events sent while denied, when the destination is later granted, then pre grant events are never delivered`() =
        runTest(testDispatcher) {
            stubConsentState(consentState(allowed = listOf("something-else")))
            plugin.setup(mockAnalytics)
            plugin.initDestination(gatedSourceConfig())

            plugin.intercept(trackEvent("pre-grant-event"))
            stubConsentState(consentState(allowed = listOf("marketing")))
            plugin.initDestination(gatedSourceConfig())

            assertTrue(plugin.isDestinationReady)
            verify(exactly = 0) { plugin.track(any()) }
        }

    // Revoke mid-session

    @Test
    fun `given a revoke mid session, when reinitialized, then no further events reach the destination`() =
        runTest(testDispatcher) {
            stubConsentState(consentState(allowed = listOf("marketing")))
            plugin.setup(mockAnalytics)
            plugin.initDestination(gatedSourceConfig())
            plugin.intercept(trackEvent("before-revoke"))

            stubConsentState(consentState(allowed = listOf("something-else")))
            plugin.initDestination(gatedSourceConfig())
            plugin.intercept(trackEvent("after-revoke"))

            assertFalse(plugin.isDestinationReady)
            verify(exactly = 1) { plugin.track(match { it.event == "before-revoke" }) }
            verify(exactly = 0) { plugin.track(match { it.event == "after-revoke" }) }
        }

    @Test
    fun `given a consent flip without reinitialization, when an event is intercepted, then the event gate drops it live`() =
        runTest(testDispatcher) {
            stubConsentState(consentState(allowed = listOf("marketing")))
            val sourceConfigState = State(initialState = SourceConfig.initialState())
            every { mockAnalytics.sourceConfigState } returns sourceConfigState
            plugin.setup(mockAnalytics)
            sourceConfigState.dispatch(SourceConfig.UpdateAction(gatedSourceConfig()))
            testDispatcher.scheduler.advanceUntilIdle()
            plugin.initDestination(gatedSourceConfig())
            plugin.intercept(trackEvent("while-granted"))

            stubConsentState(consentState(allowed = listOf("something-else")))
            plugin.intercept(trackEvent("while-revoked"))

            verify(exactly = 1) { plugin.track(match { it.event == "while-granted" }) }
            verify(exactly = 0) { plugin.track(match { it.event == "while-revoked" }) }
        }

    // Cloud mode

    @Test
    fun `given a denied destination, when an event is intercepted, then it passes through unchanged for cloud delivery`() =
        runTest(testDispatcher) {
            stubConsentState(consentState(allowed = listOf("something-else")))
            plugin.setup(mockAnalytics)
            plugin.initDestination(gatedSourceConfig())
            val event = trackEvent("cloud-bound-event")

            val returnedEvent = plugin.intercept(event)

            assertEquals(event, returnedEvent)
            verify(exactly = 0) { plugin.track(any()) }
        }

    // Grant through the event pipeline

    @Test
    fun `given a grant through the pipeline, when reinitialized, then only post grant events are delivered in order`() =
        runTest(testDispatcher) {
            val consentManagementState = State(initialState = consentState(allowed = listOf("something-else")))
            every { mockAnalytics.consentManagementState } returns consentManagementState
            val sourceConfigState = State(initialState = SourceConfig.initialState())
            every { mockAnalytics.sourceConfigState } returns sourceConfigState

            val managementPlugin = IntegrationsManagementPlugin()
            managementPlugin.setup(mockAnalytics)
            plugin.setup(mockAnalytics)
            managementPlugin.addIntegration(plugin)
            sourceConfigState.dispatch(SourceConfig.UpdateAction(gatedSourceConfig()))
            testDispatcher.scheduler.advanceUntilIdle()
            assertFalse(plugin.isDestinationReady)

            // An event's consent verdict is fixed when it is processed: pre-grant events are skipped.
            managementPlugin.intercept(trackEvent("pre-grant-event"))
            testDispatcher.scheduler.advanceUntilIdle()

            // The grant reinitializes the destination through the existing collector.
            consentManagementState.dispatch(ReplaceConsentStateAction(consentState(allowed = listOf("marketing"))))
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(plugin.isDestinationReady)

            managementPlugin.intercept(trackEvent("after-grant-1"))
            managementPlugin.intercept(trackEvent("after-grant-2"))
            testDispatcher.scheduler.advanceUntilIdle()

            verify(exactly = 0) { plugin.track(match { it.event == "pre-grant-event" }) }
            verifyOrder {
                plugin.track(match { it.event == "after-grant-1" })
                plugin.track(match { it.event == "after-grant-2" })
            }
        }

    private fun stubConsentState(state: ConsentManagementState) {
        every { mockAnalytics.consentManagementState } returns State(initialState = state)
    }

    private fun trackEvent(name: String): TrackEvent =
        TrackEvent(name, emptyJsonObject).also { applyBaseDataToEvent(it) }
}

// Replaces the consent state wholesale; the android module cannot reach the core-internal SetConsentAction.
private class ReplaceConsentStateAction(
    private val newState: ConsentManagementState
) : StateAction<ConsentManagementState> {

    override fun reduce(currentState: ConsentManagementState): ConsentManagementState = newState
}

private fun consentState(allowed: List<String>) = ConsentManagementState(
    enabled = true,
    provider = ConsentManagementProvider.CUSTOM,
    allowedConsentIds = allowed,
    deniedConsentIds = emptyList(),
)

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
