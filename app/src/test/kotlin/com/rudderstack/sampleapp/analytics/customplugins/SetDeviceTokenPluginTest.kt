package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private val deviceToken = "someDeviceToken"
private const val EVENT_NAME = "Sample Event"
private val emptyJsonObject = JsonObject(emptyMap())

class SetDeviceTokenPluginTest {

    @Test
    fun `given a device token, when it is set using custom plugin, then it is added in the payload`()
    = runTest {
        val event = provideDefaultEvent()
        val setDeviceTokenPlugin = SetDeviceTokenPlugin(deviceToken)

        setDeviceTokenPlugin.intercept(event)

        val actualDeviceToken = event.context["device"]?.jsonObject?.get("token")?.jsonPrimitive?.content
        assertEquals(deviceToken, actualDeviceToken)
    }
}

private fun provideDefaultEvent(): Event = TrackEvent(
    event = EVENT_NAME,
    properties = emptyJsonObject,
)
