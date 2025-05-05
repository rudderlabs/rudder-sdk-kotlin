package com.rudderstack.sdk.kotlin.core

import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig.Companion.serializer
import com.rudderstack.sdk.kotlin.core.internals.network.NetworkErrorStatus
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClient
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.MockBackOffPolicy
import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.handleInvalidWriteKey
import io.mockk.CapturingSlot
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

private const val downloadedSourceConfig = "config/source_config_with_single_destination.json"

class SourceConfigManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val analytics: Analytics = mockAnalytics(testScope, testDispatcher)
    private val sourceConfigState: State<SourceConfig> = mockk(relaxed = true)
    private val httpClient: HttpClient = mockk(relaxed = true)
    private lateinit var sourceConfigManager: SourceConfigManager

    @MockK
    private lateinit var mockState: State<Boolean>

    private lateinit var flowCollectorSlot: CapturingSlot<FlowCollector<Boolean>>

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        // Mock the connectivity state and capture the block.
        flowCollectorSlot = slot()
        every { analytics.connectivityState } returns mockState
        coEvery { mockState.collect(capture(flowCollectorSlot)) }

        sourceConfigManager = SourceConfigManager(analytics, sourceConfigState, httpClient)
    }

    @Test
    fun `given source config is cached in the storage, when it is fetched from the storage, then observer is notified`() =
        runTest(testDispatcher) {
            val sourceConfigString = readFileAsString(downloadedSourceConfig)
            every {
                analytics.storage.readString(
                    StorageKeys.SOURCE_CONFIG_PAYLOAD,
                    defaultVal = String.empty()
                )
            } returns sourceConfigString

            sourceConfigManager.fetchCachedSourceConfigAndNotifyObservers()

            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)
            verify(exactly = 1) {
                sourceConfigState.dispatch(match { it is SourceConfig.UpdateAction && it.updatedSourceConfig == sourceConfig })
            }
        }

    @Test
    fun `given source config is not cached in the storage, when an attempt is made to fetch it from the storage, then observer is not notified`() =
        runTest(testDispatcher) {
            every {
                analytics.storage.readString(
                    StorageKeys.SOURCE_CONFIG_PAYLOAD,
                    defaultVal = String.empty()
                )
            } returns String.empty()

            sourceConfigManager.fetchCachedSourceConfigAndNotifyObservers()

            verify(exactly = 0) {
                sourceConfigState.dispatch(any())
            }
        }

    @Test
    fun `given connection is available, when source config is fetched, then it should be stored and observer notified`() =
        runTest(testDispatcher) {
            val downloadedSourceConfigString = readFileAsString(downloadedSourceConfig)
            every { httpClient.getData() } returns Result.Success(downloadedSourceConfigString)

            sourceConfigManager.refreshSourceConfigAndNotifyObservers()
            testDispatcher.scheduler.advanceUntilIdle()
            simulateConnectionAvailability()

            LenientJson.decodeFromString<SourceConfig>(downloadedSourceConfigString).let { sourceConfig ->
                coVerify(exactly = 1) {
                    analytics.storage.write(
                        StorageKeys.SOURCE_CONFIG_PAYLOAD,
                        Json.encodeToString(serializer(), sourceConfig)
                    )
                }
                verify(exactly = 1) {
                    sourceConfigState.dispatch(match { it is SourceConfig.UpdateAction && it.updatedSourceConfig == sourceConfig })
                }
            }
        }

    @Test
    fun `given connection is unavailable, when source config is fetched, then it is neither stored nor any of the observer is notified`() =
        runTest(testDispatcher) {
            sourceConfigManager.refreshSourceConfigAndNotifyObservers()
            testDispatcher.scheduler.advanceUntilIdle()
            // As connection is not available, we are not simulating connection availability.

            coVerify(exactly = 0) {
                analytics.storage.write(any(), any<String>())
            }
            verify(exactly = 0) { sourceConfigState.dispatch(any()) }
        }

    @Test
    fun `given connection is available but network request fails, when source config is fetched, then it is neither stored nor any of the observer is notified`() =
        runTest(testDispatcher) {
            every { httpClient.getData() } returns Result.Failure(
                error = NetworkErrorStatus.ErrorRetry(),
            )

            sourceConfigManager.refreshSourceConfigAndNotifyObservers()
            testDispatcher.scheduler.advanceUntilIdle()
            simulateConnectionAvailability()

            coVerify(exactly = 0) {
                analytics.storage.write(any(), any<String>())
            }
            verify(exactly = 0) { sourceConfigState.dispatch(any()) }
        }

    @Test
    fun `given connection is available but an exception occurs while downloading source config, when source config is fetched, then it is neither stored nor any of the observer is notified`() =
        runTest(testDispatcher) {
            every { httpClient.getData() } throws Exception()

            sourceConfigManager.refreshSourceConfigAndNotifyObservers()
            testDispatcher.scheduler.advanceUntilIdle()
            simulateConnectionAvailability()

            coVerify(exactly = 0) {
                analytics.storage.write(any(), any<String>())
            }
            verify(exactly = 0) { sourceConfigState.dispatch(any()) }
        }

    @OptIn(UseWithCaution::class)
    @Test
    fun `given sourceConfig api return 400 response code, when source config is fetched, then invalid write key process in initiated`() =
        runTest(testDispatcher) {
            every { httpClient.getData() } returns Result.Failure(
                error = NetworkErrorStatus.Error400,
            )

            sourceConfigManager.refreshSourceConfigAndNotifyObservers()
            testDispatcher.scheduler.advanceUntilIdle()
            simulateConnectionAvailability()

            coVerify(exactly = 1) { analytics.handleInvalidWriteKey() }
        }

    @ParameterizedTest(name = "given sourceConfig api return {0}, when source config is fetched, then analytics instance is not shutdown and backoff is called 5 times")
    @MethodSource("provideSourceConfigErrorStatus")
    fun sourceConfigBackOffRetryTest(
        errorStatus: NetworkErrorStatus
    ) = runTest(testDispatcher) {
        val mockBackoffPolicy = spyk(MockBackOffPolicy())
        mockkStatic(::provideBackoffPolicy)
        every { provideBackoffPolicy() } returns mockBackoffPolicy

        every { httpClient.getData() } returns Result.Failure(
            error = errorStatus,
        )

        sourceConfigManager.refreshSourceConfigAndNotifyObservers()
        testDispatcher.scheduler.advanceUntilIdle()
        simulateConnectionAvailability()

        coVerify(exactly = 0) { analytics.shutdown() }
        coVerify(exactly = 5) { mockBackoffPolicy.nextDelayInMillis() }
    }

    @ParameterizedTest(name = "given sourceConfig api return {0} for first time but success the next, when source config is fetched, backoff is called 1 time and source config downloaded and stored")
    @MethodSource("provideSourceConfigErrorStatus")
    fun sourceConfigBackOffRetryTestWithSuccess(
        errorStatus: NetworkErrorStatus
    ) = runTest(testDispatcher) {
        val mockBackoffPolicy = spyk(MockBackOffPolicy())
        mockkStatic(::provideBackoffPolicy)
        every { provideBackoffPolicy() } returns mockBackoffPolicy

        val downloadedSourceConfigString = readFileAsString(downloadedSourceConfig)
        every { httpClient.getData() } returnsMany listOf(
            Result.Failure(error = errorStatus),
            Result.Success(downloadedSourceConfigString)
        )

        sourceConfigManager.refreshSourceConfigAndNotifyObservers()
        testDispatcher.scheduler.advanceUntilIdle()
        simulateConnectionAvailability()

        coVerify(exactly = 0) { analytics.shutdown() }
        coVerify(exactly = 1) { mockBackoffPolicy.nextDelayInMillis() }
        LenientJson.decodeFromString<SourceConfig>(downloadedSourceConfigString).let { sourceConfig ->
            coVerify(exactly = 1) {
                analytics.storage.write(
                    StorageKeys.SOURCE_CONFIG_PAYLOAD,
                    Json.encodeToString(serializer(), sourceConfig)
                )
            }
            verify(exactly = 1) {
                sourceConfigState.dispatch(match { it is SourceConfig.UpdateAction && it.updatedSourceConfig == sourceConfig })
            }
        }
    }

    // This setup is needed to simulate the connection availability and invoking the block.
    private fun TestScope.simulateConnectionAvailability() {
        backgroundScope.launch {
            flowCollectorSlot.captured.emit(true)
        }.also { testDispatcher.scheduler.runCurrent() }
    }

    companion object {

        @JvmStatic
        fun provideSourceConfigErrorStatus() = listOf(
            Arguments.of(NetworkErrorStatus.Error401),
            Arguments.of(NetworkErrorStatus.Error404),
            Arguments.of(NetworkErrorStatus.Error413),
            Arguments.of(NetworkErrorStatus.ErrorRetry()),
            Arguments.of(NetworkErrorStatus.ErrorUnknown),
            Arguments.of(NetworkErrorStatus.ErrorNetworkUnavailable)
        )
    }
}
