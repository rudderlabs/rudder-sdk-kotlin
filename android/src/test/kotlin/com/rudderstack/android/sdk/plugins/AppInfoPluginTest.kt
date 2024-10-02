package com.rudderstack.android.sdk.plugins

import android.app.Application
import android.content.pm.PackageManager
import com.rudderstack.android.sdk.Configuration
import com.rudderstack.android.sdk.utils.provideEvent
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.emptyJsonObject
import com.rudderstack.kotlin.sdk.internals.utils.empty
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

private const val APP_KEY = "app"
private const val APP_BUILD_KEY = "build"
private const val APP_NAME_KEY = "name"
private const val APP_NAMESPACE_KEY = "namespace"
private const val APP_VERSION_KEY = "version"

private const val APP_NAME = "RudderStack-Example"
private const val APP_NAMESPACE = "com.rudderstack.android.sdk"
private const val APP_VERSION = "1.0.3"
private const val APP_BUILD = "3"

class AppInfoPluginTest {

    @MockK
    private lateinit var mockAnalytics: Analytics

    @MockK
    private lateinit var mockApplication: Application

    private lateinit var appInfoPlugin: AppInfoPlugin

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        every { (mockAnalytics.configuration as Configuration).application } returns mockApplication

        appInfoPlugin = spyk(AppInfoPlugin())
    }

    @Test
    fun `given app context is present, when app info plugin is executed, then app info is attached to the context`() {
        val message = provideEvent()
        every { appInfoPlugin.constructAppContext(any(), any()) } returns provideAppContextPayload()

        appInfoPlugin.setup(mockAnalytics)
        appInfoPlugin.execute(message)

        val actual = message.context
        JSONAssert.assertEquals(
            provideAppContextPayload().toString(),
            actual.toString(),
            true
        )
    }

    @Test
    fun `given some context is present, when app info is merged with other context, then it is given higher priority`() {
        val message = provideEvent()
        every { appInfoPlugin.constructAppContext(any(), any()) } returns provideAppContextPayload()

        appInfoPlugin.setup(mockAnalytics)
        message.context = buildJsonObject {
            put(APP_KEY, String.empty())
        }
        appInfoPlugin.execute(message)

        val actual = message.context
        JSONAssert.assertEquals(
            provideAppContextPayload().toString(),
            actual.toString(),
            true
        )
    }

    @Test
    fun `given package info is not found, when exception occurs, then empty context is attached to the message`() {
        val message = provideEvent()
        val packageManager = mockk<PackageManager>()
        every { mockApplication.packageManager } returns packageManager
        every { packageManager.getPackageInfo(any<String>(), any<Int>()) } throws PackageManager.NameNotFoundException()

        appInfoPlugin.setup(mockAnalytics)
        appInfoPlugin.execute(message)

        val actual = message.context
        JSONAssert.assertEquals(
            emptyJsonObject.toString(),
            actual.toString(),
            true
        )
    }
}

private fun provideAppContextPayload(): JsonObject = buildJsonObject {
    put(
        APP_KEY,
        buildJsonObject {
            put(APP_NAME_KEY, APP_NAME)
            put(APP_NAMESPACE_KEY, APP_NAMESPACE)
            put(APP_VERSION_KEY, APP_VERSION)
            put(APP_BUILD_KEY, APP_BUILD)
        }
    )
}