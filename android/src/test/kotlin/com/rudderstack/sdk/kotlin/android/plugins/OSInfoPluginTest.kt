package com.rudderstack.sdk.kotlin.android.plugins

import com.rudderstack.sdk.kotlin.android.utils.provideEvent
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert


private const val OS_KEY = "os"
private const val OS_NAME_KEY = "name"
private const val OS_VERSION_KEY = "version"

private const val OS_VALUE = "Android"
private const val OS_VERSION = "11"

class OSInfoPluginTest {
    @MockK
    private lateinit var mockAnalytics: Analytics

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
    }

    @Test
    fun `given os context is present, when os info plugin is intercepted, then os info is attached to the context`() = runTest {
        val message = provideEvent()
        val osInfoPlugin = spyk(OSInfoPlugin())
        every { osInfoPlugin.constructAppContext() } returns provideOSContextPayload()

        osInfoPlugin.setup(mockAnalytics)
        osInfoPlugin.intercept(message)

        val actual = message.context
        JSONAssert.assertEquals(
            provideOSContextPayload().toString(),
            actual.toString(),
            true
        )
    }

    @Test
    fun `given os context is present, when os info is merged with other context, then os info is given higher priority`() = runTest {
        val message = provideEvent()
        val osInfoPlugin = spyk(OSInfoPlugin())
        every { osInfoPlugin.constructAppContext() } returns provideOSContextPayload()

        osInfoPlugin.setup(mockAnalytics)
        message.context = buildJsonObject {
            put(OS_KEY, String.empty())
        }
        osInfoPlugin.intercept(message)

        val actual = message.context
        JSONAssert.assertEquals(
            provideOSContextPayload().toString(),
            actual.toString(),
            true
        )
    }
}

private fun provideOSContextPayload(): JsonObject = buildJsonObject {
    put(OS_KEY, buildJsonObject {
        put(OS_NAME_KEY, OS_VALUE)
        put(OS_VERSION_KEY, OS_VERSION)
    })
}
