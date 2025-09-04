package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

private const val userAgent = "Dalvik/2.1.0 (Linux; U; Android 16; sdk_gphone64_arm64 Build/BE2A.250530.026.D1)"
private const val EVENT_NAME = "Sample Event"
private const val USER_AGENT_KEY = "userAgent"
private val emptyJsonObject = JsonObject(emptyMap())

class UserAgentPluginTest {

    @Test
    fun `given a user agent, when it is set using custom plugin, then it is added in the payload`() =
        runTest {
            val event = provideDefaultEvent()
            val userAgentPlugin = UserAgentPlugin { userAgent }

            userAgentPlugin.intercept(event)

            val actualUserAgent = event.context[USER_AGENT_KEY]?.jsonPrimitive?.content
            assertEquals(userAgent, actualUserAgent)
        }

    @Test
    fun `given null user agent, when plugin intercepts event, then no user agent is added`() =
        runTest {
            val event = provideDefaultEvent()
            val userAgentPlugin = UserAgentPlugin { null }

            userAgentPlugin.intercept(event)

            val actualUserAgent = event.context[USER_AGENT_KEY]
            assertNull(actualUserAgent)
        }
}

private fun provideDefaultEvent(): Event = TrackEvent(
    event = EVENT_NAME,
    properties = emptyJsonObject,
)
