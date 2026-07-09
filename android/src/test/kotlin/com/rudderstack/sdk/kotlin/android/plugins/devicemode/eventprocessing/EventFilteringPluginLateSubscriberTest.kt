package com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing

import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.android.utils.readFileAsString
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import io.mockk.every
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Regression coverage for the late-subscriber (warm-launch) case that historically leaked events:
 * when the source config is already present in the state *before* the plugin subscribes, and the
 * subsequent network refresh is identical (so it is de-duped by the state flow), the deny list must
 * still be applied. "Track Event 3" is blacklisted in source_config_with_black_list_event.json.
 */
class EventFilteringPluginLateSubscriberTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    private val blacklistConfig = LenientJson.decodeFromString<SourceConfig>(
        readFileAsString("eventfilteringsourceconfig/source_config_with_black_list_event.json")
    )

    @Test
    fun `given config already dispatched before the plugin subscribes and an identical refresh, when a blacklisted event is intercepted, then it is dropped`() =
        runTest(testDispatcher) {
            val state = State(initialState = SourceConfig.initialState())
            every { mockAnalytics.sourceConfigState } returns state

            // 1) Cached config is dispatched BEFORE the plugin subscribes (warm launch).
            state.dispatch(SourceConfig.UpdateAction(blacklistConfig))

            // 2) Plugin is added late and subscribes now.
            val plugin = EventFilteringPlugin("MockDestination")
            plugin.setup(mockAnalytics)
            testDispatcher.scheduler.advanceUntilIdle()

            // 3) Network refresh returns an identical config -> de-duped by the state flow.
            state.dispatch(SourceConfig.UpdateAction(blacklistConfig))
            testDispatcher.scheduler.advanceUntilIdle()

            val blacklistedEvent = TrackEvent("Track Event 3", emptyJsonObject)
            val returned = plugin.intercept(blacklistedEvent)

            assertNull(returned, "Blacklisted 'Track Event 3' must be dropped even on a late subscribe")
        }
}
