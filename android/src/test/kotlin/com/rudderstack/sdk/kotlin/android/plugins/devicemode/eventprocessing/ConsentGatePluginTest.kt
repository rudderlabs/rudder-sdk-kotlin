package com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing

import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementProvider
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.consent.ConsentManagementState
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
                enabled = true,
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
