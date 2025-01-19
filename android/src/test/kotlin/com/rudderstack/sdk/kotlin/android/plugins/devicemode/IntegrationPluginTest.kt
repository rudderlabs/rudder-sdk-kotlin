package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockDestinationCustomPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockDestinationSdk
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockDestinationIntegrationPlugin
import com.rudderstack.sdk.kotlin.android.utils.assertDoesNotThrow
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

internal const val pathToSourceConfigWithCorrectApiKey = "mockdestinationconfig/source_config_with_correct_api_key.json"
internal const val pathToSourceConfigWithIncorrectApiKey = "mockdestinationconfig/source_config_with_incorrect_api_key.json"
internal const val pathToSourceConfigWithAbsentDestinationConfig =
    "mockdestinationconfig/source_config_with_other_destination.json"
internal const val pathToSourceConfigWithDestinationDisabled =
    "mockdestinationconfig/source_config_with_disabled_destination.json"

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

    private lateinit var plugin: IntegrationPlugin

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        plugin = MockDestinationIntegrationPlugin()
        every { mockAnalytics.configuration } returns mockk<Configuration>(relaxed = true)
        plugin.setup(mockAnalytics)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `given a sourceConfig with correct destination config, when plugin is initialised with it, then destination is initialised`() =
        runTest {
            plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)
            val mockDestinationSdk = plugin.getDestinationInstance() as? MockDestinationSdk

            assertNotNull(mockDestinationSdk)
            assert(plugin.destinationState is DestinationState.Ready)
        }

    @Test
    fun `given a sourceConfig with incorrect api key for destination, when plugin is initialised with it, then destination is not initialised`() =
        runTest {
            plugin.findAndInitDestination(sourceConfigWithIncorrectApiKey)
            val mockDestinationSdk = plugin.getDestinationInstance() as? MockDestinationSdk

            assert(mockDestinationSdk == null)
            assert(plugin.destinationState is DestinationState.Failed)
            assert((plugin.destinationState as DestinationState.Failed).exception is SdkNotInitializedException)
        }

    @Test
    fun `given a sourceConfig without destination config, when plugin is initialised with it, then destination is not initialised`() =
        runTest {
            val sourceConfigWithAbsentDestinationConfig = LenientJson.decodeFromString<SourceConfig>(
                readFileAsString(pathToSourceConfigWithAbsentDestinationConfig)
            )
            plugin.findAndInitDestination(sourceConfigWithAbsentDestinationConfig)
            val mockDestinationSdk = plugin.getDestinationInstance() as? MockDestinationSdk

            assert(mockDestinationSdk == null)
            assert(plugin.destinationState is DestinationState.Failed)
            assert((plugin.destinationState as DestinationState.Failed).exception is SdkNotInitializedException)
        }

    @Test
    fun `given a sourceConfig with destination disabled, when plugin is initialised with it, then destination is not initialised`() =
        runTest {
            val sourceConfigWithDisabledDestination = LenientJson.decodeFromString<SourceConfig>(
                readFileAsString(pathToSourceConfigWithDestinationDisabled)
            )
            plugin.findAndInitDestination(sourceConfigWithDisabledDestination)
            val mockDestinationSdk = plugin.getDestinationInstance() as? MockDestinationSdk

            assert(mockDestinationSdk == null)
            assert(plugin.destinationState is DestinationState.Failed)
            assert((plugin.destinationState as DestinationState.Failed).exception is SdkNotInitializedException)
        }

    @Test
    fun `given an integration plugin for which create throws an exception, when plugin is initialised, then destination is not initialised`() =
        runTest {
            val exception = Exception("Test exception")
            val plugin = object : IntegrationPlugin() {
                override val key: String
                    get() = "MockDestination"

                override fun create(destinationConfig: JsonObject): Boolean {
                    throw exception
                }
            }
            plugin.setup(mockAnalytics)
            plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)

            assert(plugin.destinationState is DestinationState.Failed)
            assert((plugin.destinationState as DestinationState.Failed).exception == exception)
        }

    @Test
    fun `given an initialised destination and a sourceConfig, when plugin is updated with it, then destination is updated`() =
        runTest {
            plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)

            plugin.findAndUpdateDestination(sourceConfigWithCorrectApiKey)
            val updatedMockDestinationSdk = plugin.getDestinationInstance() as? MockDestinationSdk

            verify(exactly = 1) { updatedMockDestinationSdk?.update() }
        }

    @Test
    fun `given an initialised destination and its update throws an exception, when plugin is updated with a sourceConfig, then the exception is not rethrown`() =
        runTest {
            plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)

            val exception = Exception("Test exception")
            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk
            every { mockDestinationSdk.update() } throws exception

            assertDoesNotThrow { plugin.findAndUpdateDestination(sourceConfigWithCorrectApiKey) }
        }

    @Test
    fun `given an initialised destination and sourceConfig with disabled destination, when plugin is updated with it, then destination moves to Failed state`() =
        runTest {
            plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)

            val sourceConfigWithDisabledDestination = LenientJson.decodeFromString<SourceConfig>(
                readFileAsString(pathToSourceConfigWithDestinationDisabled)
            )
            plugin.findAndUpdateDestination(sourceConfigWithDisabledDestination)

            assert(plugin.destinationState is DestinationState.Failed)
            assert((plugin.destinationState as DestinationState.Failed).exception is SdkNotInitializedException)
        }

    @Test
    fun `given an initialised destination and sourceConfig without destination, when plugin is updated with it, then destination moves to Failed state`() =
        runTest {
            plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)

            val sourceConfigWithAbsentDestinationConfig = LenientJson.decodeFromString<SourceConfig>(
                readFileAsString(pathToSourceConfigWithAbsentDestinationConfig)
            )
            plugin.findAndUpdateDestination(sourceConfigWithAbsentDestinationConfig)

            assert(plugin.destinationState is DestinationState.Failed)
            assert((plugin.destinationState as DestinationState.Failed).exception is SdkNotInitializedException)
        }

    @Test
    fun `given a failed destination, when the plugin is updated with a new correct sourceConfig, then destination moves to Ready state`() = runTest {
        plugin.findAndInitDestination(sourceConfigWithIncorrectApiKey)

        plugin.findAndUpdateDestination(sourceConfigWithCorrectApiKey)
        val updatedMockDestinationSdk = plugin.getDestinationInstance() as? MockDestinationSdk

        assert(plugin.destinationState is DestinationState.Ready)
        verify(exactly = 1) { updatedMockDestinationSdk?.update() }
    }

    @Test
    fun `given an initialised integration, when its intercept called with TrackEvent, then trackEvent is called for destination`() =
        runTest {
            plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)
            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

            val event = TrackEvent("test", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { mockDestinationSdk.trackEvent("test") }
        }

    @Test
    fun `given an initialised integration, when its intercept called with ScreenEvent, then screenEvent is called for destination`() =
        runTest {
            plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)
            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

            val event = ScreenEvent("test_screen", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { mockDestinationSdk.screenEvent("test_screen") }
        }

    @Test
    fun `given an initialised integration, when its intercept called with GroupEvent, then groupEvent is called for destination`() =
        runTest {
            plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)
            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

            val event = GroupEvent("test_group_id", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { mockDestinationSdk.groupEvent("test_group_id") }
        }

    @Test
    fun `given an initialised integration, when its intercept called with IdentifyEvent, then identifyUser is called for destination`() =
        runTest {
            plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)
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
            plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)
            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

            val event = AliasEvent(previousId = "test_previous_id")
            event.userId = "test_user_id"
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { mockDestinationSdk.aliasUser("test_user_id", "test_previous_id") }
        }

    @Test
    fun `given an initialised integration, when reset called, then reset is called for destination`() = runTest {
        plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)
        val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

        plugin.reset()

        verify(exactly = 1) { mockDestinationSdk.reset() }
    }

    @Test
    fun `given an initialised integration, when flush called, then flush is called for destination`() = runTest {
        plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)
        val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

        plugin.flush()

        verify(exactly = 1) { mockDestinationSdk.flush() }
    }

    @Test
    fun `given a custom plugin, when it is added after initialisation and intercept for integration called, then the plugin's intercept is called`() =
        runTest {
            val customPlugin = spyk(MockDestinationCustomPlugin())

            plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)
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
            plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)

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

            plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)
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

            plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)
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

            plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)
            plugin.add(customPlugin)

            val returnedEvent = plugin.intercept(originalEvent)

            assertEquals(originalEvent, returnedEvent)
        }

    @Test
    fun `given a custom plugin added in integration, when it is removed, then the plugin's teardown is called`() =
        runTest {
            val customPlugin = spyk(MockDestinationCustomPlugin())

            plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)
            plugin.add(customPlugin)
            plugin.remove(customPlugin)

            verify(exactly = 1) { customPlugin.teardown() }
        }

    @Test
    fun `given an integration with custom plugin added, when integration's teardown is called, then custom plugin's teardown is also called`() =
        runTest {
            val customPlugin = spyk(MockDestinationCustomPlugin())

            plugin.findAndInitDestination(sourceConfigWithCorrectApiKey)
            plugin.add(customPlugin)
            plugin.teardown()

            verify(exactly = 1) { customPlugin.teardown() }
        }
}

internal fun applyBaseDataToEvent(event: Event) {
    event.integrations = emptyJsonObject
    event.anonymousId = "anonymousId"
    event.channel = PlatformType.Mobile
}
