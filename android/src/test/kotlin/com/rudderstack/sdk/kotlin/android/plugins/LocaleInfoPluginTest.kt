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

private const val LOCALE_KEY = "app"
private const val LOCALE_VALUE = "en-US"

class LocaleInfoPluginTest {

    @MockK
    private lateinit var mockAnalytics: Analytics

    private lateinit var localeInfoPlugin: LocaleInfoPlugin

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        localeInfoPlugin = spyk(LocaleInfoPlugin())
    }

    @Test
    fun `given locale context is present, when locale info plugin is intercepted, then locale info is attached to the context`() = runTest {
        val message = provideEvent()
        every { localeInfoPlugin.constructLocaleContext() } returns provideLocaleContextPayload()

        localeInfoPlugin.setup(mockAnalytics)
        localeInfoPlugin.intercept(message)

        val actual = message.context
        JSONAssert.assertEquals(
            provideLocaleContextPayload().toString(),
            actual.toString(),
            true
        )
    }

    @Test
    fun `given some context is present, when app info is merged with other context, then it is given higher priority`() = runTest {
        val message = provideEvent()
        every { localeInfoPlugin.constructLocaleContext() } returns provideLocaleContextPayload()

        localeInfoPlugin.setup(mockAnalytics)
        message.context = buildJsonObject {
            put(LOCALE_KEY, String.empty())
        }
        localeInfoPlugin.intercept(message)

        val actual = message.context
        JSONAssert.assertEquals(
            provideLocaleContextPayload().toString(),
            actual.toString(),
            true
        )
    }
}

private fun provideLocaleContextPayload(): JsonObject = buildJsonObject {
    put(LOCALE_KEY, LOCALE_VALUE)
}
