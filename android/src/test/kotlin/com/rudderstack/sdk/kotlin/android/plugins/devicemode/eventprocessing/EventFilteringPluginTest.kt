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
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import io.mockk.every
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val pathToSourceConfigWithWhiteListEvents =
    "eventfilteringsourceconfig/source_config_with_white_list_event.json"
private const val pathToSourceConfigWithBlackListEvents =
    "eventfilteringsourceconfig/source_config_with_black_list_event.json"
private const val pathToSourceConfigWithEmptyWhiteListEvents =
    "eventfilteringsourceconfig/source_config_with_empty_white_list_events.json"
private const val pathToSourceConfigWithEmptyBlackListEvents =
    "eventfilteringsourceconfig/source_config_with_empty_black_list_events.json"
private const val pathToSourceConfigWithEventFilteringDisabled =
    "eventfilteringsourceconfig/source_config_with_event_filtering_disabled.json"

class EventFilteringPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var eventFilteringPlugin: EventFilteringPlugin
    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    private val sourceConfigWithWhiteListEvents = LenientJson.decodeFromString<SourceConfig>(
        readFileAsString(pathToSourceConfigWithWhiteListEvents)
    )
    private val sourceConfigWithBlackListEvents = LenientJson.decodeFromString<SourceConfig>(
        readFileAsString(pathToSourceConfigWithBlackListEvents)
    )
    private val sourceConfigWithEmptyWhiteListEvents = LenientJson.decodeFromString<SourceConfig>(
        readFileAsString(pathToSourceConfigWithEmptyWhiteListEvents)
    )
    private val sourceConfigWithEmptyBlackListEvents = LenientJson.decodeFromString<SourceConfig>(
        readFileAsString(pathToSourceConfigWithEmptyBlackListEvents)
    )
    private val sourceConfigWithEventFilteringDisabled = LenientJson.decodeFromString<SourceConfig>(
        readFileAsString(pathToSourceConfigWithEventFilteringDisabled)
    )

    @BeforeEach
    fun setup() {
        eventFilteringPlugin = EventFilteringPlugin("MockDestination")

        every { mockAnalytics.sourceConfigState } returns State(initialState = SourceConfig.initialState())
        eventFilteringPlugin.setup(mockAnalytics)
    }

    @Test
    fun `given a sourceConfig with whiteListedEvents enabled, when plugin's intercept called with an event present in whiteList, then it returns that event`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithWhiteListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val allowedEvent = TrackEvent("Track Event 1", emptyJsonObject)
            val returnedEvent = eventFilteringPlugin.intercept(allowedEvent)

            assertEquals(allowedEvent, returnedEvent)
        }

    @Test
    fun `given a sourceConfig with whiteListedEvents enabled, when plugin's intercept called with an event absent in whiteList, then it returns null`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithWhiteListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val disallowedEvent = TrackEvent("Track Event 3", emptyJsonObject)
            val returnedEvent = eventFilteringPlugin.intercept(disallowedEvent)

            assertNull(returnedEvent)
        }

    @Test
    fun `given a sourceConfig with blackListedEvents enabled, when plugin's intercept is called with an event present in blackList, then it returns null`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithBlackListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val disallowedEvent = TrackEvent("Track Event 3", emptyJsonObject)
            val returnedEvent = eventFilteringPlugin.intercept(disallowedEvent)

            assertNull(returnedEvent)
        }

    @Test
    fun `given a sourceConfig with blackListedEvents enabled, when plugin's intercept is called with an event absent in blackList, then it returns that event`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithBlackListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val allowedEvent = TrackEvent("Track Event 1", emptyJsonObject)
            val returnedEvent = eventFilteringPlugin.intercept(allowedEvent)

            assertEquals(allowedEvent, returnedEvent)
        }

    @Test
    fun `given a sourceConfig with whiteListedEvents, when plugin's intercept is called with an event with leading and trailing spaces but present in list, then it returns the event`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithWhiteListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val allowedEvent = TrackEvent(" Track Event 1  ", emptyJsonObject)
            val returnedEvent = eventFilteringPlugin.intercept(allowedEvent)

            assertEquals(allowedEvent, returnedEvent)
        }

    @Test
    fun `given a sourceConfig with blackListedEvents, when plugin's intercept is called with an event with leading and trailing spaces but present in list, then it returns null`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithBlackListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val disallowedEvent = TrackEvent(" Track Event 3  ", emptyJsonObject)
            val returnedEvent = eventFilteringPlugin.intercept(disallowedEvent)

            assertNull(returnedEvent)
        }

    @Test
    fun `given a sourceConfig with empty whiteListEvents, when plugin's intercept is called with any event, then it returns null`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithEmptyWhiteListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val event1 = TrackEvent("Track Event 1", emptyJsonObject)
            val event2 = TrackEvent("Track Event 2", emptyJsonObject)
            val event3 = TrackEvent("Track Event 3", emptyJsonObject)

            assertNull(eventFilteringPlugin.intercept(event1))
            assertNull(eventFilteringPlugin.intercept(event2))
            assertNull(eventFilteringPlugin.intercept(event3))
        }

    @Test
    fun `given a sourceConfig with empty blackListEvents, when plugin's intercept is called with any event, then it returns that event`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithEmptyBlackListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val event1 = TrackEvent("Track Event 1", emptyJsonObject)
            val event2 = TrackEvent("Track Event 2", emptyJsonObject)
            val event3 = TrackEvent("Track Event 3", emptyJsonObject)

            assertEquals(event1, eventFilteringPlugin.intercept(event1))
            assertEquals(event2, eventFilteringPlugin.intercept(event2))
            assertEquals(event3, eventFilteringPlugin.intercept(event3))
        }

    @Test
    fun `given a sourceConfig with event filtering disabled, when plugin's intercept is called with any event, then it returns the same event`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithEventFilteringDisabled))
            testDispatcher.scheduler.advanceUntilIdle()

            val event1 = TrackEvent("Track Event 1", emptyJsonObject)
            val event2 = TrackEvent("Track Event 2", emptyJsonObject)
            val event3 = TrackEvent("Track Event 3", emptyJsonObject)

            assertEquals(event1, eventFilteringPlugin.intercept(event1))
            assertEquals(event2, eventFilteringPlugin.intercept(event2))
            assertEquals(event3, eventFilteringPlugin.intercept(event3))
        }

    @Test
    fun `given any sourceConfig, when plugin's intercept is called with any event other than TrackEvent, then it returns that event`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithWhiteListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val screenEvent = ScreenEvent("Track Event 1", emptyJsonObject)
            val groupEvent = GroupEvent("Track Event 1", emptyJsonObject)
            val identifyEvent = IdentifyEvent()
            identifyEvent.userId = "123"
            val aliasEvent = AliasEvent(previousId = "123")

            assertNotNull(eventFilteringPlugin.intercept(screenEvent))
            assertNotNull(eventFilteringPlugin.intercept(groupEvent))
            assertNotNull(eventFilteringPlugin.intercept(identifyEvent))
            assertNotNull(eventFilteringPlugin.intercept(aliasEvent))
        }

    @Test
    fun `given the plugin, when two different sourceConfig are emitted, then the plugin updates its internal filtering logic`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithWhiteListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val event1 = TrackEvent("Track Event 1", emptyJsonObject)
            val event2 = TrackEvent("Track Event 3", emptyJsonObject)

            assertEquals(event1, eventFilteringPlugin.intercept(event1))
            assertNull(eventFilteringPlugin.intercept(event2))

            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithBlackListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(event1, eventFilteringPlugin.intercept(event1))
            assertNull(eventFilteringPlugin.intercept(event2))
        }

    @Test
    fun `when two different sourceConfig are emitted, then plugin clears and then update the previous filtering logic`() =
        runTest(testDispatcher) {
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithWhiteListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            val event1 = TrackEvent("Track Event 1", emptyJsonObject)
            val event2 = TrackEvent("Track Event 2", emptyJsonObject)
            val event3 = TrackEvent("Track Event 3", emptyJsonObject)

            assertEquals(event1, eventFilteringPlugin.intercept(event1))
            assertEquals(event2, eventFilteringPlugin.intercept(event2))
            assertNull(eventFilteringPlugin.intercept(event3))

            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithEmptyWhiteListEvents))
            testDispatcher.scheduler.advanceUntilIdle()

            assertNull(eventFilteringPlugin.intercept(event1))
            assertNull(eventFilteringPlugin.intercept(event2))
            assertNull(eventFilteringPlugin.intercept(event3))
        }
}
