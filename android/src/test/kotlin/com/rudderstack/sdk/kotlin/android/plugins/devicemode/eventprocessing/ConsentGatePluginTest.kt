package com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing

import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.android.consent.ConsentManagementProvider
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.android.models.consent.ConsentManagementState
import com.rudderstack.sdk.kotlin.android.models.consent.consentStamp
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.ReplaceConsentStateAction
import com.rudderstack.sdk.kotlin.core.internals.models.SDKManagedContextKey
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val DESTINATION_KEY = "MockDestination"

@OptIn(ExperimentalCoroutinesApi::class)
class ConsentGatePluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    private lateinit var plugin: ConsentGatePlugin

    @BeforeEach
    fun setup() {
        plugin = ConsentGatePlugin(DESTINATION_KEY)
        every { mockAnalytics.sourceConfigState } returns State(initialState = SourceConfig.initialState())
        every { mockAnalytics.consentManagementState } returns State(
            initialState = ConsentManagementState(
                active = true,
                provider = ConsentManagementProvider.CUSTOM,
                allowedConsentIds = listOf("analytics"),
            )
        )
    }

    @Test
    fun `given a live gate, when the source config drops the consent entry, then events pass again`() =
        runTest(testDispatcher) {
            plugin.setup(mockAnalytics)
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig(gated = true)))
            testDispatcher.scheduler.advanceUntilIdle()
            assertNull(plugin.intercept(TrackEvent("gated", emptyJsonObject)))

            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig(gated = false)))
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotNull(plugin.intercept(TrackEvent("ungated", emptyJsonObject)))
        }

    @Test
    fun `given a torn down gate, when the source config changes, then the update is ignored`() =
        runTest(testDispatcher) {
            plugin.setup(mockAnalytics)
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig(gated = true)))
            testDispatcher.scheduler.advanceUntilIdle()
            assertNull(plugin.intercept(TrackEvent("before-teardown", emptyJsonObject)))

            plugin.teardown()
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig(gated = false)))
            testDispatcher.scheduler.advanceUntilIdle()

            assertNull(plugin.intercept(TrackEvent("after-teardown", emptyJsonObject)))
        }

    // Consent is judged at capture time as well as at delivery. A later grant belongs to later
    // events; it must not reach back and authorise one recorded while the destination was denied.
    @Test
    fun `given an event captured while the destination was denied, when consent is later granted, then it is still dropped`() =
        runTest(testDispatcher) {
            val denied = ConsentManagementState(
                active = true,
                provider = ConsentManagementProvider.CUSTOM,
                allowedConsentIds = listOf("something-else"),
            )
            val granted = denied.copy(allowedConsentIds = listOf("marketing"))
            val consentManagementState = State(initialState = denied)
            every { mockAnalytics.consentManagementState } returns consentManagementState

            plugin.setup(mockAnalytics)
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig(gated = true)))
            testDispatcher.scheduler.advanceUntilIdle()

            val event = TrackEvent("denied-window", emptyJsonObject).also {
                it.capturedReservedContext =
                    mapOf(SDKManagedContextKey.CONSENT_MANAGEMENT.key to denied.consentStamp)
            }

            // The user later grants exactly what this destination requires.
            consentManagementState.dispatch(ReplaceConsentStateAction(granted))

            assertNull(plugin.intercept(event))
        }

    // The other half of the pair: a revocation applies immediately, whatever the event recorded.
    @Test
    fun `given an event captured while allowed, when consent is later revoked, then it is dropped`() =
        runTest(testDispatcher) {
            val allowed = ConsentManagementState(
                active = true,
                provider = ConsentManagementProvider.CUSTOM,
                allowedConsentIds = listOf("marketing"),
            )
            val consentManagementState = State(initialState = allowed)
            every { mockAnalytics.consentManagementState } returns consentManagementState

            plugin.setup(mockAnalytics)
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig(gated = true)))
            testDispatcher.scheduler.advanceUntilIdle()

            val event = TrackEvent("allowed-window", emptyJsonObject).also {
                it.capturedReservedContext =
                    mapOf(SDKManagedContextKey.CONSENT_MANAGEMENT.key to allowed.consentStamp)
            }

            consentManagementState.dispatch(
                ReplaceConsentStateAction(allowed.copy(allowedConsentIds = listOf("something-else")))
            )

            assertNull(plugin.intercept(event))
        }

    // Events created while consent management was inactive carry nothing, so live state alone
    // decides and behaviour is unchanged for them.
    @Test
    fun `given an event carrying no captured consent, when the destination is allowed live, then it passes`() =
        runTest(testDispatcher) {
            every { mockAnalytics.consentManagementState } returns State(
                initialState = ConsentManagementState(
                    active = true,
                    provider = ConsentManagementProvider.CUSTOM,
                    allowedConsentIds = listOf("marketing"),
                )
            )
            plugin.setup(mockAnalytics)
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig(gated = true)))
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotNull(plugin.intercept(TrackEvent("no-captured-consent", emptyJsonObject)))
        }

    // The collector delivers asynchronously, so a destination registered after the source config
    // arrived is set up before its first emission. The gate must already know the destination's rules
    // there, or an unresolvable config fails open.
    @Test
    fun `given a gate set up after the source config arrived, when its first event is intercepted before the collector runs, then it is gated`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig(gated = true)))

            plugin.setup(mockAnalytics)

            // No scheduler advance: the source-config collector has not emitted yet.
            assertNull(plugin.intercept(TrackEvent("first-event", emptyJsonObject)))
        }
}

private fun sourceConfig(gated: Boolean): SourceConfig {
    val consentBlock = if (gated) {
        """, "consentManagement": [ { "provider": "custom", "consents": [ { "consent": "marketing" } ], "resolutionStrategy": "and" } ]"""
    } else {
        ""
    }
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
                "config": { "apiKey": "test-api-key"$consentBlock },
                "destinationDefinitionId": "<DESTINATION_DEFINITION_ID>",
                "destinationDefinition": { "name": "MOCK DESTINATION", "displayName": "$DESTINATION_KEY" },
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
