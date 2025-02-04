package com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing

import com.rudderstack.sdk.kotlin.android.utils.mergeWithHigherPriorityTo
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private const val MOCK_DESTINATION_KEY = "MockDestination"

class IntegrationOptionsPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var integrationOptionsPlugin: IntegrationOptionsPlugin
    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    @Before
    fun setup() {
        integrationOptionsPlugin = IntegrationOptionsPlugin(MOCK_DESTINATION_KEY)
        integrationOptionsPlugin.setup(mockAnalytics)
    }

    @Test
    fun `given an event with all destinations enabled in integration options, when plugin's intercept called with it, then it returns that event`() =
        runTest(testDispatcher) {
            val event = TrackEvent(event = "event-name", properties = emptyJsonObject)
            event.integrations = emptyJsonObject
            event.putIntegrationOption("All", true)

            val result = integrationOptionsPlugin.intercept(event)

            assertEquals(event, result)
        }

    @Test
    fun `given an event with a particular destination disabled in integration options, when plugin's intercept called with it, then it returns null`() =
        runTest(testDispatcher) {
            val event = TrackEvent(event = "event-name", properties = emptyJsonObject)
            event.integrations = emptyJsonObject
            event.putIntegrationOption(MOCK_DESTINATION_KEY, false)

            val result = integrationOptionsPlugin.intercept(event)

            assertNull(result)
        }

    @Test
    fun `given an event with all destinations disabled in integration options, when plugin's intercept called with it, then it returns null`() =
        runTest(testDispatcher) {
            val event = TrackEvent(event = "event-name", properties = emptyJsonObject)
            event.integrations = emptyJsonObject
            event.putIntegrationOption("All", false)

            val result = integrationOptionsPlugin.intercept(event)

            assertNull(result)
        }

    @Test
    fun `given an event with all but one destination enabled, when plugin's intercept called with it, then it returns that event`() =
        runTest(testDispatcher) {
            val event = TrackEvent(event = "event-name", properties = emptyJsonObject)
            event.integrations = emptyJsonObject
            event.putIntegrationOption("All", false)
            event.putIntegrationOption(MOCK_DESTINATION_KEY, true)

            val result = integrationOptionsPlugin.intercept(event)

            assertEquals(event, result)
        }

    @Test
    fun `given plugin for some other destination, when plugin's intercept called with an event which has mock destination disabled in integration options, then it returns that event`() =
        runTest(testDispatcher) {
            val integrationOptionsPlugin = IntegrationOptionsPlugin("SomeOtherDestination")
            integrationOptionsPlugin.setup(mockAnalytics)

            val event = TrackEvent(event = "event-name", properties = emptyJsonObject)
            event.integrations = emptyJsonObject
            event.putIntegrationOption("All", true)
            event.putIntegrationOption(MOCK_DESTINATION_KEY, false)

            val result = integrationOptionsPlugin.intercept(event)

            assertEquals(event, result)
        }

    @Test
    fun `given an event with empty integrations, when plugin's intercept called with it, then it returns that event`() =
        runTest(testDispatcher) {
            val event = TrackEvent(event = "event-name", properties = emptyJsonObject)
            event.integrations = emptyJsonObject

            val result = integrationOptionsPlugin.intercept(event)

            assertEquals(event, result)
        }

    @Test
    fun `given an event with integration field set to string type, when plugin's intercept called with it, then it returns that event`() =
        runTest(testDispatcher) {
            val event = TrackEvent(event = "event-name", properties = emptyJsonObject)
            event.integrations = buildJsonObject {
                put(MOCK_DESTINATION_KEY, "some string value")
            }

            val result = integrationOptionsPlugin.intercept(event)

            assertEquals(event, result)
        }

    @Test
    fun `given an event with integration field set to complex type, when plugin's intercept called with it, then it returns that event`() =
        runTest(testDispatcher) {
            val event = TrackEvent(event = "event-name", properties = emptyJsonObject)
            event.integrations = buildJsonObject {
                put(MOCK_DESTINATION_KEY, buildJsonObject {
                    put("key", "value")
                })
            }

            val result = integrationOptionsPlugin.intercept(event)

            assertEquals(event, result)
        }

    private fun Event.putIntegrationOption(key: String, value: Boolean) {
        this.integrations = this.integrations mergeWithHigherPriorityTo buildJsonObject { put(key, value) }
    }
}
