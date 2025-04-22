package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils.MockStandardDestinationIntegrationPlugin
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.android.utils.readFileAsString
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IntegrationsManagementPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)
    private val integrationPlugin = spyk(MockStandardDestinationIntegrationPlugin())
    private val sourceConfigWithCorrectApiKey = LenientJson.decodeFromString<SourceConfig>(
        readFileAsString(pathToSourceConfigWithCorrectApiKey)
    )
    private val sourceConfigWithIncorrectApiKey = LenientJson.decodeFromString<SourceConfig>(
        readFileAsString(pathToSourceConfigWithIncorrectApiKey)
    )
    private val sourceConfigWithAnotherCorrectApiKey = LenientJson.decodeFromString<SourceConfig>(
        readFileAsString(pathToSourceConfigWithAnotherCorrectApiKey)
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
    fun `given an integration plugin, when no sourceConfig is fetched and setup is called, then it is not initialised`() =
        runTest {
            integrationsManagementPlugin.setup(mockAnalytics)

            integrationsManagementPlugin.addIntegration(integrationPlugin)
            advanceUntilIdle()

            verify(exactly = 0) { integrationPlugin.initDestination(any()) }
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
    fun `given an integration, when teardown is called, then that integration's teardown is also called`() = runTest {
        integrationsManagementPlugin.setup(mockAnalytics)
        integrationsManagementPlugin.addIntegration(integrationPlugin)
        mockAnalytics.sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfigWithCorrectApiKey))
        advanceUntilIdle()

        integrationsManagementPlugin.teardown()

        verify(exactly = 1) { integrationPlugin.teardown() }
    }
}
