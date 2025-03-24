package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private val pushToken = "somePushToken"
private const val EVENT_NAME = "Sample Event"
private val emptyJsonObject = JsonObject(emptyMap())

class SetPushTokenPluginTest {

    @Test
    fun `given a push token, when it is set using custom plugin, then it is added in the payload`()
    = runTest {
        val event = provideDefaultEvent()
        val setPushTokenPlugin = SetPushTokenPlugin(pushToken)

        setPushTokenPlugin.intercept(event)

        val actualPushToken = event.context["device"]?.jsonObject?.get("token")?.jsonPrimitive?.content
        assertEquals(pushToken, actualPushToken)
    }
}

private fun provideDefaultEvent(): Event = TrackEvent(
    event = EVENT_NAME,
    properties = emptyJsonObject,
)
