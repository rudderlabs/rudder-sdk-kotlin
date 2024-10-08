package com.rudderstack.android.sdk.plugins

import com.rudderstack.android.sdk.utils.provideEvent
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.utils.empty
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

private const val TIMEZONE_KEY = "timezone"

private const val TIMEZONE_VALUE = "America/Los_Angeles"

class TimezoneInfoPluginTest {

    @MockK
    private lateinit var mockAnalytics: Analytics

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
    }

    @Test
    fun `given timezone context is present, when timezone info plugin is executed, then timezone info is attached to the context`() {
        val message = provideEvent()
        val timezoneInfoPlugin = spyk(TimezoneInfoPlugin())
        every { timezoneInfoPlugin.constructTimezoneContext() } returns provideTimezoneContextPayload()

        timezoneInfoPlugin.setup(mockAnalytics)
        timezoneInfoPlugin.execute(message)

        val actual = message.context
        JSONAssert.assertEquals(
            provideTimezoneContextPayload().toString(),
            actual.toString(),
            true
        )
    }

    @Test
    fun `given timezone context is present, when timezone info is merged with other context, then timezone info is given higher priority`() {
        val message = provideEvent()
        val timezoneInfoPlugin = spyk(TimezoneInfoPlugin())
        every { timezoneInfoPlugin.constructTimezoneContext() } returns provideTimezoneContextPayload()

        timezoneInfoPlugin.setup(mockAnalytics)
        message.context = buildJsonObject {
            put(TIMEZONE_KEY, String.empty())
        }
        timezoneInfoPlugin.execute(message)

        val actual = message.context
        JSONAssert.assertEquals(
            provideTimezoneContextPayload().toString(),
            actual.toString(),
            true
        )
    }
}

private fun provideTimezoneContextPayload(): JsonObject = buildJsonObject {
    put(TIMEZONE_KEY, TIMEZONE_VALUE)
}
