package com.rudderstack.sdk.kotlin.android.plugins

import android.app.Application
import com.rudderstack.sdk.kotlin.android.Configuration
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
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

private const val SCREEN_KEY = "screen"
private const val SCREEN_DENSITY_KEY = "density"
private const val SCREEN_HEIGHT_KEY = "height"
private const val SCREEN_WIDTH_KEY = "width"

private const val SCREEN_DENSITY = 560
private const val SCREEN_HEIGHT = 2808
private const val SCREEN_WIDTH = 1440

class ScreenInfoPluginTest {

    @MockK
    private lateinit var mockAnalytics: Analytics

    @MockK
    private lateinit var mockApplication: Application

    private lateinit var screenInfoPlugin: ScreenInfoPlugin

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        every { (mockAnalytics.configuration as Configuration).application } returns mockApplication

        screenInfoPlugin = spyk(ScreenInfoPlugin())
    }

    @Test
    fun `given screen context is present, when screen info plugin is executed, then screen info is attached to the context`() =
        runTest {
            val message = provideEvent()
            every { screenInfoPlugin.constructScreenContext(any()) } returns provideScreenContextPayload()

            screenInfoPlugin.setup(mockAnalytics)
            screenInfoPlugin.execute(message)

            val actual = message.context
            JSONAssert.assertEquals(
                provideScreenContextPayload().toString(),
                actual.toString(),
                true
            )
        }

    @Test
    fun `given screen context is present, when screen info is merged with other context, then screen info is given higher priority`() =
        runTest {
            val message = provideEvent()
            every { screenInfoPlugin.constructScreenContext(any()) } returns provideScreenContextPayload()

            screenInfoPlugin.setup(mockAnalytics)
            message.context = buildJsonObject {
                put(SCREEN_KEY, String.empty())
            }
            screenInfoPlugin.execute(message)

            val actual = message.context
            JSONAssert.assertEquals(
                provideScreenContextPayload().toString(),
                actual.toString(),
                true
            )
        }
}

private fun provideScreenContextPayload(): JsonObject = buildJsonObject {
    put(
        SCREEN_KEY,
        buildJsonObject {
            put(SCREEN_DENSITY_KEY, SCREEN_DENSITY)
            put(SCREEN_HEIGHT_KEY, SCREEN_HEIGHT)
            put(SCREEN_WIDTH_KEY, SCREEN_WIDTH)
        }
    )
}
