package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockDestinationSdk
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockLiteIntegrationPlugin
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.android.utils.readFileAsString
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("UNCHECKED_CAST")
class LiteModeIntegrationPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)
    private val sourceConfig = decodeSourceConfig(pathToSourceConfigWithCorrectApiKey)
    private val appInstance = mockk<MockDestinationSdk>(relaxed = true)

    private lateinit var plugin: MockLiteIntegrationPlugin

    @BeforeEach
    fun setup() {
        every { mockAnalytics.configuration } returns mockk<Configuration>(relaxed = true)
        plugin = MockLiteIntegrationPlugin(appInstance)
        plugin.setup(mockAnalytics)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `given a lite integration with a pending callback, when two configs arrive, then create runs once, the callback gets the app's instance, and the second config runs update`() =
        runTest {
            val callback = mockk<(Any?, DestinationResult) -> Unit>(relaxed = true)
            plugin.onDestinationReady(callback)

            verify(exactly = 0) { callback.invoke(any(), any()) }

            plugin.initDestination(sourceConfig)

            assertEquals(1, plugin.createCount)
            assertEquals(0, plugin.updateCount)
            verify(exactly = 1) { callback.invoke(appInstance, ofType(Result.Success::class) as DestinationResult) }

            val lateCallback = mockk<(Any?, DestinationResult) -> Unit>(relaxed = true)
            plugin.onDestinationReady(lateCallback)

            verify(exactly = 1) { lateCallback.invoke(appInstance, ofType(Result.Success::class) as DestinationResult) }

            plugin.initDestination(decodeSourceConfig(pathToSourceConfigWithAnotherCorrectApiKey))

            assertEquals(1, plugin.createCount)
            assertEquals(1, plugin.updateCount)
            assertTrue(plugin.isDestinationReady)
        }

    @Test
    fun `given a ready lite integration, when an event is intercepted, then the app's instance receives it`() =
        runTest {
            plugin.initDestination(sourceConfig)
            val event = TrackEvent("test", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { appInstance.trackEvent("test") }
        }

    @Test
    fun `given a lite integration with a pending callback, when the first config disables the destination, then the callback gets failure and create runs only with a later enabled config`() =
        runTest {
            val callback = mockk<(Any?, DestinationResult) -> Unit>(relaxed = true)
            plugin.onDestinationReady(callback)

            plugin.initDestination(decodeSourceConfig(pathToSourceConfigWithDestinationDisabled))

            assertEquals(0, plugin.createCount)
            assertFalse(plugin.isDestinationReady)
            verify(exactly = 1) { callback.invoke(any(), ofType(Result.Failure::class) as DestinationResult) }

            plugin.initDestination(sourceConfig)

            assertEquals(1, plugin.createCount)
            assertTrue(plugin.isDestinationReady)
        }

    @Test
    fun `given a ready lite integration, when the destination is disabled and enabled again, then the SDK calls update and not create`() =
        runTest {
            plugin.initDestination(sourceConfig)

            plugin.initDestination(decodeSourceConfig(pathToSourceConfigWithDestinationDisabled))

            assertFalse(plugin.isDestinationReady)

            plugin.initDestination(sourceConfig)

            assertEquals(1, plugin.createCount)
            assertEquals(2, plugin.updateCount)
            assertTrue(plugin.isDestinationReady)
        }

    @Test
    fun `given a lite integration whose create throws, when the next config arrives, then the callback gets failure and create runs again`() =
        runTest {
            val callback = mockk<(Any?, DestinationResult) -> Unit>(relaxed = true)
            plugin.onDestinationReady(callback)
            plugin.createError = IllegalStateException("Create failed")

            plugin.initDestination(sourceConfig)

            assertFalse(plugin.isDestinationReady)
            verify(exactly = 1) { callback.invoke(any(), ofType(Result.Failure::class) as DestinationResult) }

            plugin.createError = null
            plugin.initDestination(sourceConfig)

            assertEquals(2, plugin.createCount)
            assertEquals(0, plugin.updateCount)
            assertTrue(plugin.isDestinationReady)
        }

    @Test
    fun `given a custom integration that holds an instance before the first config, when the config arrives, then create runs and the pending callback gets success`() =
        runTest {
            val customInstance = mockk<MockDestinationSdk>(relaxed = true)
            var createCount = 0
            val customPlugin = object : IntegrationPlugin() {
                override val key: String
                    get() = "MockDestination"

                override fun create(destinationConfig: JsonObject) {
                    createCount++
                }

                override fun getDestinationInstance(): Any = customInstance
            }
            customPlugin.setup(mockAnalytics)
            val callback = mockk<(Any?, DestinationResult) -> Unit>(relaxed = true)
            customPlugin.onDestinationReady(callback)

            verify(exactly = 0) { callback.invoke(any(), any()) }

            customPlugin.initDestination(sourceConfig)

            assertEquals(1, createCount)
            assertTrue(customPlugin.isDestinationReady)
            verify(exactly = 1) { callback.invoke(customInstance, ofType(Result.Success::class) as DestinationResult) }
        }

    @Test
    fun `given pending callbacks where a later one throws, when the destination is created, then the earlier callback runs only once and the next config calls update`() =
        runTest {
            val firstCallback = mockk<(Any?, DestinationResult) -> Unit>(relaxed = true)
            plugin.onDestinationReady(firstCallback)
            plugin.onDestinationReady { _, _ -> throw IllegalStateException("Callback failed") }

            plugin.initDestination(sourceConfig)

            verify(exactly = 1) { firstCallback.invoke(any(), any()) }

            plugin.initDestination(sourceConfig)

            assertEquals(1, plugin.createCount)
            assertEquals(1, plugin.updateCount)
            assertTrue(plugin.isDestinationReady)
        }

    private fun decodeSourceConfig(path: String): SourceConfig =
        LenientJson.decodeFromString<SourceConfig>(readFileAsString(path))
}
