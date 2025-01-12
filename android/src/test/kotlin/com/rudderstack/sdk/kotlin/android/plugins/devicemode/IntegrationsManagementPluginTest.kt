package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockDestinationIntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockDestinationSdk
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.android.utils.readFileAsString
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.FlowState
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalCoroutinesApi::class)
class IntegrationsManagementPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)
    private val integrationPlugin = spyk(MockDestinationIntegrationPlugin())

    private val plugin = IntegrationsManagementPlugin()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { mockAnalytics.configuration } returns mockk<Configuration>(relaxed = true)
        every { mockAnalytics.sourceConfigState } returns FlowState(initialState = SourceConfig.initialState())
        plugin.setup(mockAnalytics)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `given an integration, when it is added using addIntegration before sourceConfig is fetched, then destination is initialised`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

            plugin.addIntegration(integrationPlugin)
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig))
            advanceUntilIdle()

            verify(exactly = 1) { integrationPlugin.initialize(sourceConfig) }
        }

    @Test
    fun `given an integration, when it is added using addIntegration after sourceConfig is fetched, then destination is initialised`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig))
            plugin.addIntegration(integrationPlugin)
            advanceUntilIdle()

            verify(exactly = 1) { integrationPlugin.initialize(sourceConfig) }
        }

    @Test
    fun `given an integration, when a callback is registered before sourceConfig is fetched and integration is added, then it is called after successful initialisation`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

            val callback = mockk<(Any?, DestinationResult) -> Unit>(relaxed = true)

            plugin.onDestinationReady(integrationPlugin, callback)
            plugin.addIntegration(integrationPlugin)
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig))
            advanceUntilIdle()

            val mockDestinationSdk = integrationPlugin.getUnderlyingInstance() as? MockDestinationSdk

            verify(exactly = 1) { callback.invoke(mockDestinationSdk, ofType(Result.Success::class) as DestinationResult) }
        }

    @Test
    fun `given an integration, when a callback is registered before sourceConfig is fetched but after integration is added, then it is called after successful initialisation`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

            val callback = mockk<(Any?, DestinationResult) -> Unit>(relaxed = true)

            plugin.addIntegration(integrationPlugin)
            plugin.onDestinationReady(integrationPlugin, callback)
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig))
            advanceUntilIdle()

            val mockDestinationSdk = integrationPlugin.getUnderlyingInstance() as? MockDestinationSdk

            verify(exactly = 1) { callback.invoke(mockDestinationSdk, ofType(Result.Success::class) as DestinationResult) }
        }

    @Test
    fun `given an integration, when a callback is registered after sourceConfig is fetched and integration is added, then it is called after successful initialisation`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

            val callback = mockk<(Any?, DestinationResult) -> Unit>(relaxed = true)

            plugin.addIntegration(integrationPlugin)
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig))
            plugin.onDestinationReady(integrationPlugin, callback)
            advanceUntilIdle()

            val mockDestinationSdk = integrationPlugin.getUnderlyingInstance() as? MockDestinationSdk

            verify(exactly = 1) { callback.invoke(mockDestinationSdk, ofType(Result.Success::class) as DestinationResult) }
        }

    @Test
    fun `given an initialised integration, when flush is called, then integration plugin's flush is called`() = runTest {
        val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

        plugin.addIntegration(integrationPlugin)
        mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig))
        advanceUntilIdle()

        plugin.flush()

        verify(exactly = 1) { integrationPlugin.flush() }
    }

    @Test
    fun `given a failed integration, when flush is called, then integration plugin's flush is not called`() = runTest {
        val sourceConfigString = readFileAsString(sourceConfigWithIncorrectApiKey)
        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

        plugin.addIntegration(integrationPlugin)
        mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig))
        advanceUntilIdle()

        plugin.flush()

        verify(exactly = 0) { integrationPlugin.flush() }
    }

    @Test
    fun `given an initialised integration, when reset is called, then integration plugin's reset is called`() = runTest {
        val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

        plugin.addIntegration(integrationPlugin)
        mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig))
        advanceUntilIdle()

        plugin.reset()

        verify(exactly = 1) { integrationPlugin.reset() }
    }

    @Test
    fun `given a failed integration, when reset is called, then integration plugin's reset is not called`() = runTest {
        val sourceConfigString = readFileAsString(sourceConfigWithIncorrectApiKey)
        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

        plugin.addIntegration(integrationPlugin)
        mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig))
        advanceUntilIdle()

        plugin.reset()

        verify(exactly = 0) { integrationPlugin.reset() }
    }

    @Test
    fun `given an integration, when intercept is called max_queue_size times before sourceConfig is fetched, then integration's intercept is called after it is fetched`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)
            val events = mutableListOf<TrackEvent>()

            plugin.addIntegration(integrationPlugin)
            repeat(MAX_QUEUE_SIZE) {
                val event = TrackEvent("test event $it", emptyJsonObject)
                applyBaseDataToEvent(event)
                events.add(event)
                plugin.intercept(event)
            }

            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig))
            advanceUntilIdle()

            events.forEach {
                coVerify(exactly = 1) { integrationPlugin.intercept(it) }
            }
        }

    @Test
    fun `given an integration, when intercept is called more than max_queue_size times, then integration's intercept is called for latest max_queue_size events`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)
            val events = mutableListOf<TrackEvent>()
            val eventsOverflowCount = 5

            plugin.addIntegration(integrationPlugin)
            repeat(MAX_QUEUE_SIZE + eventsOverflowCount) {
                val event = TrackEvent("test event $it", emptyJsonObject)
                applyBaseDataToEvent(event)
                events.add(event)
                plugin.intercept(event)
            }

            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig))
            advanceUntilIdle()

            events.forEachIndexed { index, event ->
                if (index >= eventsOverflowCount) {
                    coVerify(exactly = 1) { integrationPlugin.intercept(event) }
                } else {
                    coVerify(exactly = 0) { integrationPlugin.intercept(event) }
                }
            }
        }

    @Test
    fun `given an integration, when teardown is called, then that integration's teardown is also called`() = runTest {
        val sourceConfigString = readFileAsString(sourceConfigWithCorrectApiKey)
        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

        plugin.addIntegration(integrationPlugin)
        mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig))
        advanceUntilIdle()

        plugin.teardown()

        verify(exactly = 1) { integrationPlugin.teardown() }
    }
}
