package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockCustomIntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockDestinationSdk
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.android.utils.readFileAsString
import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("UNCHECKED_CAST")
class CustomIntegrationPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    private lateinit var plugin: MockCustomIntegrationPlugin
    private val sourceConfig = SourceConfig.initialState()

    @BeforeEach
    fun setup() {
        plugin = spyk(MockCustomIntegrationPlugin())
        mockInitialiseSdk()
        every { mockAnalytics.configuration } returns mockk<Configuration>(relaxed = true)
        plugin.setup(mockAnalytics)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `given any sourceConfig, when a custom integration plugin is initialised with it, then integration is ready`() =
        runTest(testDispatcher) {
            plugin.initDestination(sourceConfig)
            val mockDestinationSdk = plugin.getDestinationInstance() as? MockDestinationSdk

            assertNotNull(mockDestinationSdk)
            assertTrue(plugin.isDestinationReady)
        }

    @Test
    fun `given an initialised integration, when its intercept called with TrackEvent, then trackEvent is called for destination`() =
        runTest(testDispatcher) {
            plugin.initDestination(sourceConfig)
            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

            val event = TrackEvent("test", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { mockDestinationSdk.trackEvent("test") }
        }

    @Test
    fun `given an initialised integration, when its intercept called with ScreenEvent, then screenEvent is called for destination`() =
        runTest {
            plugin.initDestination(sourceConfig)
            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

            val event = ScreenEvent("test_screen", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { mockDestinationSdk.screenEvent("test_screen") }
        }

    @Test
    fun `given an initialised integration, when its intercept called with GroupEvent, then groupEvent is called for destination`() =
        runTest {
            plugin.initDestination(sourceConfig)
            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

            val event = GroupEvent("test_group_id", emptyJsonObject)
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { mockDestinationSdk.groupEvent("test_group_id") }
        }

    @Test
    fun `given an initialised integration, when its intercept called with IdentifyEvent, then identifyUser is called for destination`() =
        runTest {
            plugin.initDestination(sourceConfig)
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
            plugin.initDestination(sourceConfig)
            val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

            val event = AliasEvent(previousId = "test_previous_id")
            event.userId = "test_user_id"
            applyBaseDataToEvent(event)

            plugin.intercept(event)

            verify(exactly = 1) { mockDestinationSdk.aliasUser("test_user_id", "test_previous_id") }
        }

    @Test
    fun `given an initialised integration, when reset called, then reset is called for destination`() = runTest {
        plugin.initDestination(sourceConfig)
        val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

        plugin.reset()

        verify(exactly = 1) { mockDestinationSdk.reset() }
    }

    @Test
    fun `given an initialised integration, when flush called, then flush is called for destination`() = runTest {
        plugin.initDestination(sourceConfig)
        val mockDestinationSdk = plugin.getDestinationInstance() as MockDestinationSdk

        plugin.flush()

        verify(exactly = 1) { mockDestinationSdk.flush() }
    }

    @Test
    fun `given an initialised integration, when a callback is registered for it, then it is called immediately with success result`() =
        runTest {
            plugin.initDestination(sourceConfig)

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

            plugin.initDestination(sourceConfig)

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

            plugin.initDestination(sourceConfig)

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
            every { plugin.initialiseMockSdk(any()) } throws IllegalArgumentException("Some error")

            plugin.initDestination(sourceConfig)

            verify(exactly = 1) { callback.invoke(null, ofType(Result.Failure::class) as DestinationResult) }
        }

    @Test
    fun `when an integration is initialized again with different sourceConfig, then update is not called`() =
        runTest {
            val sourceConfigWithAnotherCorrectApiKey = LenientJson.decodeFromString<SourceConfig>(
                readFileAsString(pathToSourceConfigWithAnotherCorrectApiKey)
            )
            plugin.initDestination(sourceConfig)

            plugin.initDestination(sourceConfigWithAnotherCorrectApiKey)

            verify(exactly = 0) { plugin.update(any()) }
        }

    private fun mockInitialiseSdk() {
        every { plugin.initialiseMockSdk(any()) } returns mockk(relaxed = true)
    }
}
