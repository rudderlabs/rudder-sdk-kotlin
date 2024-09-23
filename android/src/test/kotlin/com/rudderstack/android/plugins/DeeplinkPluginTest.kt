package com.rudderstack.android.plugins

import android.app.Activity
import android.app.Application
import com.rudderstack.android.Configuration
import com.rudderstack.android.utils.mockAnalytics
import com.rudderstack.android.utils.mockUri
import com.rudderstack.core.internals.models.RudderOption
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
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
import org.junit.After
import org.junit.Before
import org.junit.Test

class DeeplinkPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val plugin = DeeplinkPlugin()

    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockActivity: Activity

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)

        every { mockAnalytics.track(any<String>(), any<JsonObject>(), any<RudderOption>()) } returns Unit
        every { mockApplication.registerActivityLifecycleCallbacks(plugin) } returns Unit
        every { mockApplication.unregisterActivityLifecycleCallbacks(plugin) } returns Unit

        every { mockActivity.intent.data } returns mockUri()
        every { mockActivity.referrer } returns mockUri(scheme = "app", host = "testApplication")

        pluginSetup()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given trackDeeplinks is false, when pluginSetup called again, then registerActivityLifecycleCallbacks is not called again`() =
        runTest {
            pluginSetup(trackingEnabled = false)

            verify(exactly = 1) { mockApplication.registerActivityLifecycleCallbacks(plugin) }
        }

    @Test
    fun `given trackDeeplinks is true, when pluginSetup called again, then registerActivityLifecycleCallbacks is called again`() =
        runTest {
            pluginSetup(trackingEnabled = true)

            verify(exactly = 2) { mockApplication.registerActivityLifecycleCallbacks(plugin) }
        }

    @Test
    fun `when onActivityCreated is called, then Deeplink opened event called with correct properties`() = runTest {
        val eventProperties = buildJsonObject {
            put(REFERRING_APPLICATION_KEY, "app://testApplication")
            put(URL_KEY, "https://www.test.com")
        }

        plugin.onActivityCreated(mockActivity, null)

        verify(exactly = 1) {
            mockAnalytics.track(name = DEEPLINK_OPENED_KEY, properties = eq(eventProperties), options = eq(RudderOption()))
        }
    }

    @Test
    fun `when teardown is called, then unregisterActivityLifecycleCallbacks is called`() = runTest(testDispatcher) {
        plugin.teardown()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { mockApplication.unregisterActivityLifecycleCallbacks(plugin) }
    }

    @Test
    fun `given a uri with query params, when onActivityCreated is called, then Deeplink opened event has query params also in properties`() =
        runTest {
            every { mockActivity.intent.data } returns mockUri(
                queryParameters = mapOf("param1" to "value1", "param2" to "value2"),
                isHierarchical = true
            )
            val eventProperties = buildJsonObject {
                put(REFERRING_APPLICATION_KEY, "app://testApplication")
                put(URL_KEY, "https://www.test.com?param1=value1&param2=value2")
                put("param1", "value1")
                put("param2", "value2")
            }

            plugin.onActivityCreated(mockActivity, null)

            verify(exactly = 1) {
                mockAnalytics.track(
                    name = DEEPLINK_OPENED_KEY,
                    properties = eq(eventProperties),
                    options = eq(RudderOption())
                )
            }
        }

    private fun pluginSetup(trackingEnabled: Boolean = true) {

        val mockConfiguration = mockk<Configuration> {
            every { application } returns mockApplication
            every { trackDeeplinks } returns trackingEnabled
        }

        every { mockAnalytics.configuration } returns mockConfiguration

        plugin.setup(analytics = mockAnalytics)
    }
}
