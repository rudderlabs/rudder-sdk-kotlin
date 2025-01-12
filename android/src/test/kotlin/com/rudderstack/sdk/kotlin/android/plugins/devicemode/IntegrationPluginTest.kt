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
import org.junit.After
import org.junit.Before
import org.junit.Test

internal const val sourceConfigWithCorrectApiKey = "mockdestinationconfig/source_config_with_correct_api_key.json"
internal const val sourceConfigWithIncorrectApiKey = "mockdestinationconfig/source_config_with_incorrect_api_key.json"

@OptIn(ExperimentalCoroutinesApi::class)
class IntegrationPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

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
    fun `given a destination, when initialize called with correct source config, then destination is initialised`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

            plugin.initialize(sourceConfig)
            val mockDestinationSdk = plugin.getUnderlyingInstance() as? MockDestinationSdk

            assert(mockDestinationSdk != null)
            assert(plugin.destinationState is DestinationState.Ready)
        }

    @Test
    fun `given a destination, when initialize called with incorrect source config, then destination is not initialised`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfigWithIncorrectApiKey)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

            plugin.initialize(sourceConfig)
            val mockDestinationSdk = plugin.getUnderlyingInstance() as? MockDestinationSdk

            assert(mockDestinationSdk == null)
            assert(plugin.destinationState is DestinationState.Failed)
            assert((plugin.destinationState as DestinationState.Failed).exception is SdkNotInitializedException)
        }

    @Test
    fun `given an initialised destination, when intercept called with TrackEvent, then trackEvent is called for destination`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

            plugin.initialize(sourceConfig)
            val mockDestinationSdk = plugin.getUnderlyingInstance() as MockDestinationSdk

            val event = TrackEvent("test", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { mockDestinationSdk.trackEvent("test") }
        }

    @Test
    fun `given an initialised destination, when intercept called with ScreenEvent, then screenEvent is called for destination`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

            plugin.initialize(sourceConfig)
            val mockDestinationSdk = plugin.getUnderlyingInstance() as MockDestinationSdk

            val event = ScreenEvent("test_screen", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { mockDestinationSdk.screenEvent("test_screen") }
        }

    @Test
    fun `given an initialised destination, when intercept called with GroupEvent, then groupEvent is called for destination`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

            plugin.initialize(sourceConfig)
            val mockDestinationSdk = plugin.getUnderlyingInstance() as MockDestinationSdk

            val event = GroupEvent("test_group_id", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { mockDestinationSdk.groupEvent("test_group_id") }
        }

    @Test
    fun `given an initialised destination, when intercept called with IdentifyEvent, then identifyUser is called for destination`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

            plugin.initialize(sourceConfig)
            val mockDestinationSdk = plugin.getUnderlyingInstance() as MockDestinationSdk

            val event = IdentifyEvent()
            event.userId = "test_user_id"
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { mockDestinationSdk.identifyUser("test_user_id") }
        }

    @Test
    fun `given an initialised destination, when intercept called with AliasEvent, then aliasUser is called for destination`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

            plugin.initialize(sourceConfig)
            val mockDestinationSdk = plugin.getUnderlyingInstance() as MockDestinationSdk

            val event = AliasEvent(previousId = "test_previous_id")
            event.userId = "test_user_id"
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { mockDestinationSdk.aliasUser("test_user_id", "test_previous_id") }
        }

    @Test
    fun `given an initialised destination, when reset called, then reset is called for destination`() = runTest {
        val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

        plugin.initialize(sourceConfig)
        val mockDestinationSdk = plugin.getUnderlyingInstance() as MockDestinationSdk

        plugin.reset()

        verify(exactly = 1) { mockDestinationSdk.reset() }
    }

    @Test
    fun `given an initialised destination, when flush called, then flush is called for destination`() = runTest {
        val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

        plugin.initialize(sourceConfig)
        val mockDestinationSdk = plugin.getUnderlyingInstance() as MockDestinationSdk

        plugin.flush()

        verify(exactly = 1) { mockDestinationSdk.flush() }
    }

    @Test
    fun `given a destination integration, when a plugin is added after initialisation, then the plugin's intercept is called when an event is sent`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)
            val customPlugin = spyk(MockDestinationCustomPlugin())

            plugin.initialize(sourceConfig)
            plugin.add(customPlugin)

            val event = TrackEvent("test", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            coVerify(exactly = 1) { customPlugin.intercept(event) }
        }

    @Test
    fun `given a destination integration, when a plugin is added before initialisation, then the plugin's intercept is called when an event is sent`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)
            val customPlugin = spyk(MockDestinationCustomPlugin())

            plugin.add(customPlugin)
            plugin.initialize(sourceConfig)

            val event = TrackEvent("test", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            coVerify(exactly = 1) { customPlugin.intercept(event) }
        }

    @Test
    fun `given a destination integration, when a plugin is removed after initialisation, then the plugin's teardown is called`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)
            val customPlugin = spyk(MockDestinationCustomPlugin())

            plugin.initialize(sourceConfig)
            plugin.add(customPlugin)
            plugin.remove(customPlugin)

            verify(exactly = 1) { customPlugin.teardown() }
        }

    @Test
    fun `given a destination integration, when a plugin is removed before initialisation, then the plugin's teardown is not called`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)
            val customPlugin = spyk(MockDestinationCustomPlugin())

            plugin.add(customPlugin)
            plugin.remove(customPlugin)
            plugin.initialize(sourceConfig)

            verify(exactly = 0) { customPlugin.teardown() }
        }

}

internal fun applyBaseDataToEvent(event: Event) {
    event.integrations = emptyJsonObject
    event.anonymousId = "anonymousId"
    event.channel = PlatformType.Mobile
}
