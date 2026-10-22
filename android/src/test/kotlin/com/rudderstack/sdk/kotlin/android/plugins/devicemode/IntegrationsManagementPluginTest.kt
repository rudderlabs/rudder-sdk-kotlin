package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockDestinationSdk
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockLiteIntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockStandardIntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.ReplaceConsentStateAction
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.android.utils.readFileAsString
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import kotlinx.serialization.json.JsonObject
import com.rudderstack.sdk.kotlin.android.models.consent.ConsentManagementState
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IntegrationsManagementPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)
    private val integrationPlugin = spyk(MockStandardIntegrationPlugin())
    private val sourceConfigWithCorrectApiKey = LenientJson.decodeFromString<SourceConfig>(
        readFileAsString(pathToSourceConfigWithCorrectApiKey)
    )
    private val sourceConfigWithIncorrectApiKey = LenientJson.decodeFromString<SourceConfig>(
        readFileAsString(pathToSourceConfigWithIncorrectApiKey)
    )
    private val sourceConfigWithAnotherCorrectApiKey = LenientJson.decodeFromString<SourceConfig>(
        readFileAsString(pathToSourceConfigWithAnotherCorrectApiKey)
    )

    private val disabledSourceConfig = sourceConfigWithCorrectApiKey.copy(
        source = sourceConfigWithCorrectApiKey.source.copy(isSourceEnabled = false)
    )

    private val integrationsManagementPlugin = IntegrationsManagementPlugin()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { mockAnalytics.configuration } returns mockk<Configuration>(relaxed = true)
        every { mockAnalytics.sourceConfigState } returns State(initialState = SourceConfig.initialState())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `given an integration plugin, when it is added before sourceConfig is fetched and setup is called, then it is initialised`() =
        runTest {
            integrationsManagementPlugin.setup(mockAnalytics)

            integrationsManagementPlugin.addIntegration(integrationPlugin)
            advanceUntilIdle()
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithCorrectApiKey))
            advanceUntilIdle()

            verify(exactly = 1) { integrationPlugin.initDestination(sourceConfigWithCorrectApiKey) }
        }

    @Test
    fun `given an integration plugin, when it is added after sourceConfig is fetched and setup is called, then it is initialised`() =
        runTest {
            integrationsManagementPlugin.setup(mockAnalytics)

            advanceUntilIdle()
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithCorrectApiKey))
            advanceUntilIdle()
            integrationsManagementPlugin.addIntegration(integrationPlugin)
            advanceUntilIdle()

            verify(exactly = 1) { integrationPlugin.initDestination(sourceConfigWithCorrectApiKey) }
        }

    @Test
    fun `given sourceConfig is dispatched before setup is called, when an integration is added, then it is initialised`() =
        runTest {
            // Config is already present in the state BEFORE the plugin subscribes (warm launch / late subscriber).
            // This would fail with the previous dropInitialState() implementation, which discarded the cached config.
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithCorrectApiKey))

            integrationsManagementPlugin.setup(mockAnalytics)
            advanceUntilIdle()

            integrationsManagementPlugin.addIntegration(integrationPlugin)
            advanceUntilIdle()

            verify(exactly = 1) { integrationPlugin.initDestination(sourceConfigWithCorrectApiKey) }
        }

    @Test
    fun `given an integration plugin, when no sourceConfig is fetched and setup is called, then it is not initialised`() =
        runTest {
            integrationsManagementPlugin.setup(mockAnalytics)

            integrationsManagementPlugin.addIntegration(integrationPlugin)
            advanceUntilIdle()

            verify(exactly = 0) { integrationPlugin.initDestination(any()) }
        }

    @Test
    fun `given a disabled source config, when consent changes, then destinations are not reinitialised`() =
        runTest {
            integrationsManagementPlugin.setup(mockAnalytics)
            integrationsManagementPlugin.addIntegration(integrationPlugin)
            advanceUntilIdle()
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithCorrectApiKey))
            advanceUntilIdle()
            verify(exactly = 1) { integrationPlugin.initDestination(any()) }

            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(disabledSourceConfig))
            advanceUntilIdle()
            mockAnalytics.consentManagementState.dispatch(
                ReplaceConsentStateAction(ConsentManagementState(active = true, allowedConsentIds = listOf("marketing")))
            )
            advanceUntilIdle()

            verify(exactly = 1) { integrationPlugin.initDestination(any()) }
        }

    @Test
    fun `given an added integration plugin, when it is removed, then its teardown is called`() = runTest {
        integrationsManagementPlugin.setup(mockAnalytics)
        integrationsManagementPlugin.addIntegration(integrationPlugin)
        mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithCorrectApiKey))
        advanceUntilIdle()

        integrationsManagementPlugin.removeIntegration(integrationPlugin)

        verify(exactly = 1) { integrationPlugin.teardown() }
    }

    @Test
    fun `given an integration whose teardown does not call super, when it is removed and added again, then it is created again`() =
        runTest {
            var createCount = 0
            var instance: Any? = null
            val plugin = object : IntegrationPlugin() {
                override val key: String
                    get() = "MockDestination"

                override fun create(destinationConfig: JsonObject) {
                    createCount++
                    instance = Any()
                }

                override fun getDestinationInstance(): Any? = instance

                override fun teardown() {
                    instance = null
                }
            }
            integrationsManagementPlugin.setup(mockAnalytics)
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithCorrectApiKey))
            advanceUntilIdle()
            integrationsManagementPlugin.addIntegration(plugin)
            advanceUntilIdle()

            integrationsManagementPlugin.removeIntegration(plugin)

            assertFalse(plugin.isDestinationReady)

            integrationsManagementPlugin.addIntegration(plugin)
            advanceUntilIdle()

            assertEquals(2, createCount)
            assertTrue(plugin.isDestinationReady)
        }

    @Test
    fun `given an integration plugin, when sourceConfig is emitted multiple times, then integration is updated`() =
        runTest {
            integrationsManagementPlugin.setup(mockAnalytics)

            integrationsManagementPlugin.addIntegration(integrationPlugin)
            advanceUntilIdle()
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithCorrectApiKey))
            advanceUntilIdle()

            verify(exactly = 1) { integrationPlugin.initDestination(sourceConfigWithCorrectApiKey) }

            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithAnotherCorrectApiKey))
            advanceUntilIdle()

            verify(exactly = 1) { integrationPlugin.initDestination(sourceConfigWithAnotherCorrectApiKey) }
        }

    @Test
    fun `given an initialised integration, when flush is called for management plugin, then integration plugin's flush is called`() =
        runTest {
            integrationsManagementPlugin.setup(mockAnalytics)
            integrationsManagementPlugin.addIntegration(integrationPlugin)
            advanceUntilIdle()
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithCorrectApiKey))
            advanceUntilIdle()

            integrationsManagementPlugin.flush()
            advanceUntilIdle()

            verify(exactly = 1) { integrationPlugin.flush() }
        }

    @Test
    fun `given a failed integration, when flush is called for management plugin, then integration plugin's flush is not called`() =
        runTest {
            integrationsManagementPlugin.setup(mockAnalytics)
            integrationsManagementPlugin.addIntegration(integrationPlugin)
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithIncorrectApiKey))
            advanceUntilIdle()

            integrationsManagementPlugin.flush()
            advanceUntilIdle()

            verify(exactly = 0) { integrationPlugin.flush() }
        }

    @Test
    fun `given an initialised integration, when reset is called for management plugin, then integration plugin's reset is called`() =
        runTest {
            integrationsManagementPlugin.setup(mockAnalytics)
            integrationsManagementPlugin.addIntegration(integrationPlugin)
            advanceUntilIdle()
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithCorrectApiKey))
            advanceUntilIdle()

            integrationsManagementPlugin.reset()
            advanceUntilIdle()

            verify(exactly = 1) { integrationPlugin.reset() }
        }

    @Test
    fun `given a failed integration, when reset is called for management plugin, then integration plugin's reset is not called`() =
        runTest {
            integrationsManagementPlugin.setup(mockAnalytics)
            integrationsManagementPlugin.addIntegration(integrationPlugin)
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithIncorrectApiKey))
            advanceUntilIdle()

            integrationsManagementPlugin.reset()
            advanceUntilIdle()

            verify(exactly = 0) { integrationPlugin.reset() }
        }

    @Test
    fun `given an integration, when intercept is called max_queue_size times before sourceConfig is fetched, then integration's intercept is called after it is fetched`() =
        runTest {
            val events = mutableListOf<TrackEvent>()

            integrationsManagementPlugin.setup(mockAnalytics)
            integrationsManagementPlugin.addIntegration(integrationPlugin)
            repeat(MAX_QUEUE_SIZE) {
                val event = TrackEvent("test event $it", emptyJsonObject)
                applyBaseDataToEvent(event)
                events.add(event)
                integrationsManagementPlugin.intercept(event)
            }

            advanceUntilIdle()
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithCorrectApiKey))
            advanceUntilIdle()

            events.forEach {
                coVerify(exactly = 1) { integrationPlugin.intercept(it) }
            }
        }

    @Test
    fun `given an integration, when intercept is called more than max_queue_size times, then integration's intercept is called for latest max_queue_size events`() =
        runTest {
            val events = mutableListOf<TrackEvent>()
            val eventsOverflowCount = 5

            integrationsManagementPlugin.setup(mockAnalytics)
            integrationsManagementPlugin.addIntegration(integrationPlugin)
            repeat(MAX_QUEUE_SIZE + eventsOverflowCount) {
                val event = TrackEvent("test event $it", emptyJsonObject)
                applyBaseDataToEvent(event)
                events.add(event)
                integrationsManagementPlugin.intercept(event)
            }

            advanceUntilIdle()
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithCorrectApiKey))
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
    fun `given two integrations where the first throws in track, when events are processed, then the second gets every event and the error names the destination and event`() =
        runTest {
            val failingInstance = mockk<MockDestinationSdk>(relaxed = true)
            every { failingInstance.trackEvent(any()) } throws IllegalStateException("Track failed")
            val nextInstance = mockk<MockDestinationSdk>(relaxed = true)
            integrationsManagementPlugin.setup(mockAnalytics)
            integrationsManagementPlugin.addIntegration(MockLiteIntegrationPlugin(failingInstance))
            integrationsManagementPlugin.addIntegration(MockLiteIntegrationPlugin(nextInstance))
            mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithCorrectApiKey))
            advanceUntilIdle()
            val firstEvent = TrackEvent("first", emptyJsonObject).also { applyBaseDataToEvent(it) }
            val secondEvent = TrackEvent("second", emptyJsonObject).also { applyBaseDataToEvent(it) }

            integrationsManagementPlugin.intercept(firstEvent)
            integrationsManagementPlugin.intercept(secondEvent)
            advanceUntilIdle()

            verify(exactly = 1) { nextInstance.trackEvent("first") }
            verify(exactly = 1) { nextInstance.trackEvent("second") }
            verify(exactly = 1) { failingInstance.trackEvent("second") }
            val logger = mockAnalytics.logger
            verify(exactly = 1) {
                logger.error(
                    match { it.contains("MockDestination") && it.contains(firstEvent.messageId) },
                    ofType(IllegalStateException::class)
                )
            }
        }

    @Test
    fun `given an integration, when teardown is called, then that integration's teardown is also called`() = runTest {
        integrationsManagementPlugin.setup(mockAnalytics)
        integrationsManagementPlugin.addIntegration(integrationPlugin)
        mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithCorrectApiKey))
        advanceUntilIdle()

        integrationsManagementPlugin.teardown()

        verify(exactly = 1) { integrationPlugin.teardown() }
    }
}
