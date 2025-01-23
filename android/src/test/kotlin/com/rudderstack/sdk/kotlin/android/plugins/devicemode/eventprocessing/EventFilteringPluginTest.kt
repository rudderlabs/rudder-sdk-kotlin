package com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing

import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.android.utils.readFileAsString
import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.FlowState
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import io.mockk.every
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private const val pathToSourceConfigWithWhiteListEvents =
    "eventfilteringsourceconfig/source_config_with_white_list_event.json"
private const val pathToSourceConfigWithBlackListEvents =
    "eventfilteringsourceconfig/source_config_with_black_list_event.json"

class EventFilteringPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var plugin: EventFilteringPlugin
    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    private val sourceConfigWithWhiteListEvents = LenientJson.decodeFromString<SourceConfig>(
        readFileAsString(pathToSourceConfigWithWhiteListEvents)
    )
    private val sourceConfigWithBlackListEvents = LenientJson.decodeFromString<SourceConfig>(
        readFileAsString(pathToSourceConfigWithBlackListEvents)
    )

    @Before
    fun setup() {
        plugin = EventFilteringPlugin("MockDestination")

        every { mockAnalytics.sourceConfigState } returns FlowState(initialState = SourceConfig.initialState())
        plugin.setup(mockAnalytics)
    }

    @Test
    fun `given a sourceConfig with whiteListedEvents enabled, when plugin's intercept called with an event present in whiteList, then it returns that event`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithWhiteListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val allowedEvent = TrackEvent("Track Event 1", emptyJsonObject)
            val returnedEvent = plugin.intercept(allowedEvent)

            assertEquals(allowedEvent, returnedEvent)
        }

    @Test
    fun `given a sourceConfig with whiteListedEvents enabled, when plugin's intercept called with an event absent in whiteList, then it returns null`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithWhiteListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val disallowedEvent = TrackEvent("Track Event 3", emptyJsonObject)
            val returnedEvent = plugin.intercept(disallowedEvent)

            assertNull(returnedEvent)
        }

    @Test
    fun `given a sourceConfig with blackListedEvents enabled, when plugin's intercept is called with an event present in blackList, then it returns null`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithBlackListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val disallowedEvent = TrackEvent("Track Event 2", emptyJsonObject)
            val returnedEvent = plugin.intercept(disallowedEvent)

            assertNull(returnedEvent)
        }

    @Test
    fun `given a sourceConfig with blackListedEvents enabled, when plugin's intercept is called with an event absent in blackList, then it returns that event`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithBlackListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val allowedEvent = TrackEvent("Track Event 3", emptyJsonObject)
            val returnedEvent = plugin.intercept(allowedEvent)

            assertEquals(allowedEvent, returnedEvent)
        }

    @Test
    fun `given a sourceConfig with whiteListedEvents, when plugin's intercept is called with an event with leading and trailing spaces but present in list, then it returns the event`() =
        runTest {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithWhiteListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val allowedEvent = TrackEvent(" Track Event 1  ", emptyJsonObject)
            val returnedEvent = plugin.intercept(allowedEvent)

            assertEquals(allowedEvent, returnedEvent)
        }

    @Test
    fun `given a sourceConfig with blackListedEvents, when plugin's intercept is called with an event with leading and trailing spaces but present in list, then it returns null`() =
        runTest {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithBlackListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val disallowedEvent = TrackEvent(" Track Event 2  ", emptyJsonObject)
            val returnedEvent = plugin.intercept(disallowedEvent)

            assertNull(returnedEvent)
        }

    @Test
    fun `given any sourceConfig, when plugin's intercept is called with any event other than TrackEvent, then it returns that event`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithWhiteListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val screenEvent = ScreenEvent("Screen Event 1", emptyJsonObject)
            val groupEvent = GroupEvent("Group Event 1", emptyJsonObject)
            val identifyEvent = IdentifyEvent()
            identifyEvent.userId = "123"
            val aliasEvent = AliasEvent(previousId = "123")

            assertNotNull(plugin.intercept(screenEvent))
            assertNotNull(plugin.intercept(groupEvent))
            assertNotNull(plugin.intercept(identifyEvent))
            assertNotNull(plugin.intercept(aliasEvent))
        }

    @Test
    fun `given the plugin, when two different sourceConfig are emitted, then the plugin updates its internal filtering logic`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithWhiteListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val event1 = TrackEvent("Track Event 1", emptyJsonObject)
            val event2 = TrackEvent("Track Event 3", emptyJsonObject)

            assertEquals(event1, plugin.intercept(event1))
            assertNull(plugin.intercept(event2))

            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithBlackListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            assertNull(plugin.intercept(event1))
            assertEquals(event2, plugin.intercept(event2))
        }
}
