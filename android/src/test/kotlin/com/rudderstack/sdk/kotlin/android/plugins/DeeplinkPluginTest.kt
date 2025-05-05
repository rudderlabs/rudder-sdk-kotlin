package com.rudderstack.sdk.kotlin.android.plugins

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.storage.CheckBuildVersionUseCase
import com.rudderstack.sdk.kotlin.android.utils.addLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.android.utils.mockUri
import com.rudderstack.sdk.kotlin.android.utils.removeLifecycleObserver
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics

class DeeplinkPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockActivity: Activity

    private lateinit var plugin: DeeplinkPlugin

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)

        plugin = DeeplinkPlugin()
        every { mockAnalytics.track(any<String>(), any<JsonObject>(), any<RudderOption>()) } returns Unit

        every { mockActivity.intent.data } returns mockUri()
        every { mockActivity.referrer } returns mockUri(scheme = "app", host = "testApplication")
        every { mockActivity.intent.getParcelableExtra<Uri?>(Intent.EXTRA_REFERRER) } returns mockUri(
            scheme = "app",
            host = "testApplication"
        )
        every { (mockAnalytics as AndroidAnalytics).addLifecycleObserver(plugin) } just Runs
        mockkObject(CheckBuildVersionUseCase)
        every { CheckBuildVersionUseCase.isAndroidVersionAtLeast(Build.VERSION_CODES.LOLLIPOP_MR1) } returns true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
        unmockkObject(CheckBuildVersionUseCase)
    }

    @Test
    fun `given trackDeepLinks is false, when pluginSetup is called, then addLifecycleObserver should not be called`() =
        runTest {
            val trackingEnabled = false
            val mockConfiguration = mockk<Configuration> {
                every { application } returns mockApplication
                every { trackDeepLinks } returns trackingEnabled
            }
            every { mockAnalytics.configuration } returns mockConfiguration

            plugin.setup(analytics = mockAnalytics)

            verify(exactly = 0) { (mockAnalytics as AndroidAnalytics).addLifecycleObserver(plugin) }
        }

    @Test
    fun `given trackDeepLinks is true, when pluginSetup called again, then addLifecycleObserver is called once`() =
        runTest {
            val trackingEnabled = true
            val mockConfiguration = mockk<Configuration> {
                every { application } returns mockApplication
                every { trackDeepLinks } returns trackingEnabled
            }
            every { mockAnalytics.configuration } returns mockConfiguration

            plugin.setup(analytics = mockAnalytics)

            verify(exactly = 1) { (mockAnalytics as AndroidAnalytics).addLifecycleObserver(plugin) }
        }

    @Test
    fun `given trackDeepLinks is enabled, when onActivityCreated is called, then Deeplink opened event called with correct properties`() =
        runTest {
            val trackingEnabled = true
            val mockConfiguration = mockk<Configuration> {
                every { application } returns mockApplication
                every { trackDeepLinks } returns trackingEnabled
            }
            every { mockAnalytics.configuration } returns mockConfiguration

            plugin.setup(analytics = mockAnalytics)
            plugin.onActivityCreated(mockActivity, null)

            val eventProperties = buildJsonObject {
                put(REFERRING_APPLICATION_KEY, "app://testApplication")
                put(URL_KEY, "https://www.test.com")
            }
            verify(exactly = 1) {
                mockAnalytics.track(
                    name = DEEPLINK_OPENED_KEY,
                    properties = eq(eventProperties),
                    options = eq(RudderOption())
                )
            }
        }

    @Test
    fun `given trackDeepLinks is enabled and api level is 21, when onActivityCreated is called, then Deeplink opened event called with correct properties`() =
        runTest {
            val trackingEnabled = true
            every { CheckBuildVersionUseCase.isAndroidVersionAtLeast(Build.VERSION_CODES.LOLLIPOP_MR1) } returns false
            val mockConfiguration = mockk<Configuration> {
                every { application } returns mockApplication
                every { trackDeepLinks } returns trackingEnabled
            }
            every { mockAnalytics.configuration } returns mockConfiguration

            plugin.setup(analytics = mockAnalytics)
            plugin.onActivityCreated(mockActivity, null)

            val eventProperties = buildJsonObject {
                put(REFERRING_APPLICATION_KEY, "app://testApplication")
                put(URL_KEY, "https://www.test.com")
            }
            verify(exactly = 1) {
                mockAnalytics.track(
                    name = DEEPLINK_OPENED_KEY,
                    properties = eq(eventProperties),
                    options = eq(RudderOption())
                )
            }
        }

    @Test
    fun `given trackDeepLinks is enabled and uri with query params is passed, when onActivityCreated is called, then Deeplink opened event has query params also in properties`() =
        runTest {
            val trackingEnabled = true
            val mockConfiguration = mockk<Configuration> {
                every { application } returns mockApplication
                every { trackDeepLinks } returns trackingEnabled
            }
            every { mockAnalytics.configuration } returns mockConfiguration
            every { mockActivity.intent.data } returns mockUri(
                queryParameters = mapOf("param1" to "value1", "param2" to "value2"),
                isHierarchical = true
            )

            plugin.setup(analytics = mockAnalytics)
            plugin.onActivityCreated(mockActivity, null)

            val eventProperties = buildJsonObject {
                put(REFERRING_APPLICATION_KEY, "app://testApplication")
                put(URL_KEY, "https://www.test.com?param1=value1&param2=value2")
                put("param1", "value1")
                put("param2", "value2")
            }
            verify(exactly = 1) {
                mockAnalytics.track(
                    name = DEEPLINK_OPENED_KEY,
                    properties = eq(eventProperties),
                    options = eq(RudderOption())
                )
            }
        }

    @Test
    fun `when teardown called, then removeLifecycleObserver is called`() = runTest {
        plugin.setup(mockAnalytics)

        plugin.teardown()

        verify { (mockAnalytics as AndroidAnalytics).removeLifecycleObserver(plugin) }
    }
}
