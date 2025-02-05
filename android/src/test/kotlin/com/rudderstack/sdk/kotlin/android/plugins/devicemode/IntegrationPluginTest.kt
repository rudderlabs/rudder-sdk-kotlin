package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockDestinationCustomPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockDestinationSdk
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockDestinationIntegrationPlugin
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.android.utils.readFileAsString
import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.serialization.json.put

internal const val pathToSourceConfigWithCorrectApiKey = "mockdestinationconfig/source_config_with_correct_api_key.json"
internal const val pathToSourceConfigWithAnotherCorrectApiKey =
    "mockdestinationconfig/source_config_with_another_correct_api_key.json"
internal const val pathToSourceConfigWithIncorrectApiKey = "mockdestinationconfig/source_config_with_incorrect_api_key.json"
internal const val pathToSourceConfigWithAbsentDestinationConfig =
    "mockdestinationconfig/source_config_with_other_destination.json"
internal const val pathToSourceConfigWithDestinationDisabled =
    "mockdestinationconfig/source_config_with_disabled_destination.json"

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalCoroutinesApi::class)
class IntegrationPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)
    private val sourceConfigWithCorrectApiKey = LenientJson.decodeFromString<SourceConfig>(
        readFileAsString(pathToSourceConfigWithCorrectApiKey)
    )
    private val sourceConfigWithIncorrectApiKey = LenientJson.decodeFromString<SourceConfig>(
        readFileAsString(pathToSourceConfigWithIncorrectApiKey)
    )
    private val apiKeyRegex = Regex("[^a-zA-Z0-9-]")

    private lateinit var plugin: MockDestinationIntegrationPlugin

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        plugin = spyk(MockDestinationIntegrationPlugin())
        mockInitialiseSdk()
        every { mockAnalytics.configuration } returns mockk<Configuration>(relaxed = true)
        plugin.setup(mockAnalytics)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `given a sourceConfig with correct destination config, when plugin is initialised with it, then integration is ready`() =
        runTest {
            plugin.initDestination(sourceConfigWithCorrectApiKey)
            val mockDestinationSdk = plugin.getDestinationInstance() as? MockDestinationSdk

            assertNotNull(mockDestinationSdk)
            assertTrue(plugin.isDestinationReady)
        }

    @Test
    fun `given a sourceConfig with incorrect api key for destination, when plugin is initialised with it, then integration is not ready`() =
        runTest {
            plugin.initDestination(sourceConfigWithIncorrectApiKey)
            val mockDestinationSdk = plugin.getDestinationInstance() as? MockDestinationSdk

            assert(mockDestinationSdk == null)
            assertFalse(plugin.isDestinationReady)
        }

    @Test
    fun `given a sourceConfig without destination config, when plugin is initialised with it, then integration is not ready`() =
        runTest {
            val sourceConfigWithAbsentDestinationConfig = LenientJson.decodeFromString<SourceConfig>(
                readFileAsString(pathToSourceConfigWithAbsentDestinationConfig)
            )
            plugin.initDestination(sourceConfigWithAbsentDestinationConfig)
            val mockDestinationSdk = plugin.getDestinationInstance() as? MockDestinationSdk

            assert(mockDestinationSdk == null)
            assertFalse(plugin.isDestinationReady)
        }

    @Test
    fun `given a sourceConfig with destination disabled, when plugin is initialised with it, then integration is not ready`() =
        runTest {
            val sourceConfigWithDisabledDestination = LenientJson.decodeFromString<SourceConfig>(
                readFileAsString(pathToSourceConfigWithDestinationDisabled)
            )
            plugin.initDestination(sourceConfigWithDisabledDestination)
            val mockDestinationSdk = plugin.getDestinationInstance() as? MockDestinationSdk

            assert(mockDestinationSdk == null)
            assertFalse(plugin.isDestinationReady)
        }

    @Test
    fun `given an integration plugin for which create throws an exception, when plugin is initialised, then integration is not ready`() =
        runTest {
            val exception = Exception("Test exception")
            val plugin = object : IntegrationPlugin() {
                override val key: String
                    get() = "MockDestination"

                override fun create(destinationConfig: JsonObject) {
                    throw exception
                }

                override fun getDestinationInstance(): Any? {
                    return null
                }
            }
            plugin.setup(mockAnalytics)
            plugin.initDestination(sourceConfigWithCorrectApiKey)

            assertFalse(plugin.isDestinationReady)
        }

    @Test
    fun `given an initialised integration, when plugin is updated with a sourceConfig, then destinationConfig in integration is updated`() =
        runTest {
            val sourceConfigWithAnotherCorrectApiKey = LenientJson.decodeFromString<SourceConfig>(
                readFileAsString(pathToSourceConfigWithAnotherCorrectApiKey)
            )
            plugin.initDestination(sourceConfigWithCorrectApiKey)
            val initialDestinationConfig = plugin.destinationConfig

            plugin.initDestination(sourceConfigWithAnotherCorrectApiKey)
            val destinationConfig = plugin.destinationConfig

            assertNotEquals(initialDestinationConfig, destinationConfig)
        }

    @Test
    fun `given an initialised integration, when plugin is updated with a sourceConfig with disabled destination, then integration becomes not ready`() =
        runTest {
            plugin.initDestination(sourceConfigWithCorrectApiKey)

            val sourceConfigWithDisabledDestination = LenientJson.decodeFromString<SourceConfig>(
                readFileAsString(pathToSourceConfigWithDestinationDisabled)
            )
            plugin.initDestination(sourceConfigWithDisabledDestination)

            assertFalse(plugin.isDestinationReady)
        }

    @Test
    fun `given an initialised integration, when plugin is updated with sourceConfig without destination, then integration becomes not ready`() =
        runTest {
            plugin.initDestination(sourceConfigWithCorrectApiKey)

            val sourceConfigWithAbsentDestinationConfig = LenientJson.decodeFromString<SourceConfig>(
                readFileAsString(pathToSourceConfigWithAbsentDestinationConfig)
            )
            plugin.initDestination(sourceConfigWithAbsentDestinationConfig)

            assertFalse(plugin.isDestinationReady)
        }

    @Test
    fun `given an integration which is not ready, when the plugin is updated with a new correct sourceConfig, then integration becomes ready`() =
        runTest {
            plugin.initDestination(sourceConfigWithIncorrectApiKey)
            assertFalse(plugin.isDestinationReady)

            plugin.initDestination(sourceConfigWithCorrectApiKey)
            assertTrue(plugin.isDestinationReady)
        }

    @Test
    fun `given an initialised integration, when its intercept called with TrackEvent, then trackEvent is called for destination`() =
        runTest {
            plugin.initDestination(sourceConfigWithCorrectApiKey)
            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

            val event = TrackEvent("test", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { mockDestinationSdk.trackEvent("test") }
        }

    @Test
    fun `given an initialised integration, when its intercept called with ScreenEvent, then screenEvent is called for destination`() =
        runTest {
            plugin.initDestination(sourceConfigWithCorrectApiKey)
            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

            val event = ScreenEvent("test_screen", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { mockDestinationSdk.screenEvent("test_screen") }
        }

    @Test
    fun `given an initialised integration, when its intercept called with GroupEvent, then groupEvent is called for destination`() =
        runTest {
            plugin.initDestination(sourceConfigWithCorrectApiKey)
            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

            val event = GroupEvent("test_group_id", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { mockDestinationSdk.groupEvent("test_group_id") }
        }

    @Test
    fun `given an initialised integration, when its intercept called with IdentifyEvent, then identifyUser is called for destination`() =
        runTest {
            plugin.initDestination(sourceConfigWithCorrectApiKey)
            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

            val event = IdentifyEvent()
            event.userId = "test_user_id"
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { mockDestinationSdk.identifyUser("test_user_id") }
        }

    @Test
    fun `given an initialised integration, when its intercept called with AliasEvent, then aliasUser is called for destination`() =
        runTest {
            plugin.initDestination(sourceConfigWithCorrectApiKey)
            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

            val event = AliasEvent(previousId = "test_previous_id")
            event.userId = "test_user_id"
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { mockDestinationSdk.aliasUser("test_user_id", "test_previous_id") }
        }

    @Test
    fun `given an initialised integration, when reset called, then reset is called for destination`() = runTest {
        plugin.initDestination(sourceConfigWithCorrectApiKey)
        val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

        plugin.reset()

        verify(exactly = 1) { mockDestinationSdk.reset() }
    }

    @Test
    fun `given an initialised integration, when flush called, then flush is called for destination`() = runTest {
        plugin.initDestination(sourceConfigWithCorrectApiKey)
        val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

        plugin.flush()

        verify(exactly = 1) { mockDestinationSdk.flush() }
    }

    @Test
    fun `given an initialised integration, when a callback is registered for it, then it is called immediately with success result`() =
        runTest {
            plugin.initDestination(sourceConfigWithCorrectApiKey)

            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk
            val callback = mockk<(Any?, DestinationResult) -> Unit>(relaxed = true)
            plugin.onDestinationReady(callback)

            verify(exactly = 1) { callback.invoke(mockDestinationSdk, ofType(Result.Success::class) as DestinationResult) }
        }

    @Test
    fun `given an uninitialised integration, when a callback is registered for it and then it is initialised, then callback is called with success result`() =
        runTest {
            val callback = mockk<(Any?, DestinationResult) -> Unit>(relaxed = true)
            plugin.onDestinationReady(callback)

            plugin.initDestination(sourceConfigWithCorrectApiKey)

            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk
            verify(exactly = 1) { callback.invoke(mockDestinationSdk, ofType(Result.Success::class) as DestinationResult) }
        }

    @Test
    fun `given an integration, when multiple callbacks are registered for it, then all the callbacks are called`() =
        runTest {
            val callback1 = mockk<(Any?, DestinationResult) -> Unit>(relaxed = true)
            val callback2 = mockk<(Any?, DestinationResult) -> Unit>(relaxed = true)
            val callback3 = mockk<(Any?, DestinationResult) -> Unit>(relaxed = true)
            plugin.onDestinationReady(callback1)
            plugin.onDestinationReady(callback2)
            plugin.onDestinationReady(callback3)

            plugin.initDestination(sourceConfigWithCorrectApiKey)

            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk
            verify(exactly = 1) { callback1.invoke(mockDestinationSdk, ofType(Result.Success::class) as DestinationResult) }
            verify(exactly = 1) { callback2.invoke(mockDestinationSdk, ofType(Result.Success::class) as DestinationResult) }
            verify(exactly = 1) { callback3.invoke(mockDestinationSdk, ofType(Result.Success::class) as DestinationResult) }
        }

    @Test
    fun `given an uninitialised integration, when a callback is registered for it and then it fails to initialise, then callback is called with failure result`() =
        runTest {
            val callback = mockk<(Any?, DestinationResult) -> Unit>(relaxed = true)
            plugin.onDestinationReady(callback)

            plugin.initDestination(sourceConfigWithIncorrectApiKey)

            verify(exactly = 1) { callback.invoke(null, ofType(Result.Failure::class) as DestinationResult) }
        }

    @Test
    fun `given a custom plugin, when it is added after initialisation and intercept for integration called, then the plugin's intercept is called`() =
        runTest {
            val customPlugin = spyk(MockDestinationCustomPlugin())

            plugin.initDestination(sourceConfigWithCorrectApiKey)
            plugin.add(customPlugin)

            val event = TrackEvent("test", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            coVerify(exactly = 1) { customPlugin.intercept(event) }
        }

    @Test
    fun `given a custom plugin, when it is added before initialisation and intercept for integration called, then the plugin's intercept is called`() =
        runTest {
            val customPlugin = spyk(MockDestinationCustomPlugin())

            plugin.add(customPlugin)
            plugin.initDestination(sourceConfigWithCorrectApiKey)

            val event = TrackEvent("test", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            coVerify(exactly = 1) { customPlugin.intercept(event) }
        }

    @Test
    fun `given a custom plugin which modifies event, when it is added to an integration, then destination's event api is called with modified event`() =
        runTest {
            val customPlugin = spyk(MockDestinationCustomPlugin())
            val modifiedEvent = TrackEvent("modified_event", emptyJsonObject)
            applyBaseDataToEvent(modifiedEvent)
            coEvery { customPlugin.intercept(any()) } returns modifiedEvent

            plugin.initDestination(sourceConfigWithCorrectApiKey)
            plugin.add(customPlugin)
            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

            val event = TrackEvent("test", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            coVerify(exactly = 1) { mockDestinationSdk.trackEvent(modifiedEvent.event) }
        }

    @Test
    fun `given a custom plugin which drops the event, when it is added to an integration, then destination's event api is not called`() =
        runTest {
            val customPlugin = spyk(MockDestinationCustomPlugin())
            coEvery { customPlugin.intercept(any()) } returns null

            plugin.initDestination(sourceConfigWithCorrectApiKey)
            plugin.add(customPlugin)
            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

            val event = TrackEvent("test", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 0) { mockDestinationSdk.trackEvent(any()) }
        }

    @Test
    fun `given a custom plugin which modifies event, when it is added to an integration, then integration still returns the original event`() =
        runTest {
            val customPlugin = spyk(MockDestinationCustomPlugin())
            val modifiedEvent = TrackEvent("modified_event", emptyJsonObject)
            applyBaseDataToEvent(modifiedEvent)
            coEvery { customPlugin.intercept(any()) } returns modifiedEvent

            val originalEvent = TrackEvent("test", emptyJsonObject)
            applyBaseDataToEvent(originalEvent)

            plugin.initDestination(sourceConfigWithCorrectApiKey)
            plugin.add(customPlugin)

            val returnedEvent = plugin.intercept(originalEvent)

            assertEquals(originalEvent, returnedEvent)
        }

    @Test
    fun `given a custom plugin added in integration, when it is removed, then the plugin's teardown is called`() =
        runTest {
            val customPlugin = spyk(MockDestinationCustomPlugin())

            plugin.initDestination(sourceConfigWithCorrectApiKey)
            plugin.add(customPlugin)
            plugin.remove(customPlugin)

            verify(exactly = 1) { customPlugin.teardown() }
        }

    @Test
    fun `given an integration with custom plugin added, when integration's teardown is called, then custom plugin's teardown is also called`() =
        runTest {
            val customPlugin = spyk(MockDestinationCustomPlugin())

            plugin.initDestination(sourceConfigWithCorrectApiKey)
            plugin.add(customPlugin)
            plugin.teardown()

            verify(exactly = 1) { customPlugin.teardown() }
        }

    private fun mockInitialiseSdk() {
        every { plugin.initialiseMockSdk(match { it.contains(apiKeyRegex) }) } throws IllegalArgumentException("Invalid API key")
        every { plugin.initialiseMockSdk(match { !it.contains(apiKeyRegex) }) } answers {
            spyk(MockDestinationSdk.initialise(arg<String>(0)))
        }
    }
}

internal fun applyBaseDataToEvent(event: Event) {
    event.integrations = buildJsonObject {
        put("All", true)
    }
    event.anonymousId = "anonymousId"
    event.channel = PlatformType.Mobile
}
