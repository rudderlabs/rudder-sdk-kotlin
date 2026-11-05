package com.rudderstack.sdk.kotlin.core

import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig.Companion.serializer
import com.rudderstack.sdk.kotlin.core.internals.network.NetworkErrorStatus
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClient
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.DateTimeUtils
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.MockBackOffPolicy
import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.handleInvalidWriteKey
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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

    private val connectivityFlow = MutableStateFlow(false)

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        // Back the connectivity state with a MutableStateFlow so tests can toggle availability.
        connectivityFlow.value = false
        every { analytics.connectivityState } returns mockState
        every { mockState.flow } returns connectivityFlow

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

            sourceConfigManager.fetchSourceConfigAtLaunchAndNotifyObservers()
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
            sourceConfigManager.fetchSourceConfigAtLaunchAndNotifyObservers()
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

            sourceConfigManager.fetchSourceConfigAtLaunchAndNotifyObservers()
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

            sourceConfigManager.fetchSourceConfigAtLaunchAndNotifyObservers()
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

            sourceConfigManager.fetchSourceConfigAtLaunchAndNotifyObservers()
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

        sourceConfigManager.fetchSourceConfigAtLaunchAndNotifyObservers()
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

        sourceConfigManager.fetchSourceConfigAtLaunchAndNotifyObservers()
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
        connectivityFlow.value = true
        testDispatcher.scheduler.runCurrent()
    }

    @Test
    fun `given mobile platform, when getQuery is called, then it should contain correct query params`() = runTest(testDispatcher) {
        val testWriteKey = "test-write-key-12345"
        val testVersion = "1.0.0"
        val testBuildVersion = "100"
        
        every { analytics.getPlatformType() } returns PlatformType.Mobile
        every { analytics.configuration.writeKey } returns testWriteKey
        every { analytics.storage.getLibraryVersion().getVersionName() } returns testVersion
        every { analytics.storage.getLibraryVersion().getBuildVersion() } returns testBuildVersion

        val query = analytics.getQuery()

        assertEquals("android", query["p"])
        assertEquals(testVersion, query["v"])
        assertEquals(testWriteKey, query["writeKey"])
        assertEquals(testBuildVersion, query["bv"])
        assertEquals(4, query.size)
    }

    @Test
    fun `given server platform, when getQuery is called, then it should contain correct query params`() = runTest(testDispatcher) {
        val testWriteKey = "test-write-key-67890"
        val testVersion = "2.0.0"
        
        every { analytics.getPlatformType() } returns PlatformType.Server
        every { analytics.configuration.writeKey } returns testWriteKey
        every { analytics.storage.getLibraryVersion().getVersionName() } returns testVersion

        val query = analytics.getQuery()

        assertEquals("kotlin", query["p"])
        assertEquals(testVersion, query["v"])
        assertEquals(testWriteKey, query["writeKey"])
        assertEquals(3, query.size)
    }

    @Nested
    inner class PeriodicRefresh {

        private val interval = 1.hours.inWholeMilliseconds
        private var clockOffsetMillis = 0L

        @BeforeEach
        fun setUpClock() {
            mockkObject(DateTimeUtils)
            every { DateTimeUtils.getSystemCurrentTime() } answers {
                testDispatcher.scheduler.currentTime - clockOffsetMillis
            }
            every { httpClient.getData() } returns Result.Success(readFileAsString(downloadedSourceConfig))
        }

        @AfterEach
        fun tearDownClock() {
            unmockkObject(DateTimeUtils)
        }

        @Test
        fun `given a launch fetch, when the app stays in the foreground, then the loop fetches once per interval`() =
            runRefreshTest {
                completeLaunchFetch()

                sourceConfigManager.startPeriodicRefresh()
                sourceConfigManager.startPeriodicRefresh()
                advanceBy(interval - 1)
                verify(exactly = 1) { httpClient.getData() }

                advanceBy(1)
                verify(exactly = 2) { httpClient.getData() }

                advanceBy(interval)
                verify(exactly = 3) { httpClient.getData() }
            }

        @Test
        fun `given the last fetch is older than the interval, when the app comes to the foreground, then the loop fetches at once`() =
            runRefreshTest {
                completeLaunchFetch()
                advanceBy(interval)

                sourceConfigManager.startPeriodicRefresh()
                advanceBy(0)

                verify(exactly = 2) { httpClient.getData() }
            }

        @Test
        fun `given the last fetch is newer than the interval, when the app comes to the foreground, then the loop waits for the rest of the interval`() =
            runRefreshTest {
                completeLaunchFetch()
                advanceBy(interval / 2)

                sourceConfigManager.startPeriodicRefresh()
                advanceBy(interval / 2 - 1)
                verify(exactly = 1) { httpClient.getData() }

                advanceBy(1)
                verify(exactly = 2) { httpClient.getData() }
            }

        @Test
        fun `given the launch fetch waits for the network, when the loop starts, then the loop waits for the launch fetch`() =
            runRefreshTest {
                sourceConfigManager.fetchSourceConfigAtLaunchAndNotifyObservers()
                sourceConfigManager.startPeriodicRefresh()
                advanceBy(0)
                verify(exactly = 0) { httpClient.getData() }

                simulateConnectionAvailability()
                advanceBy(interval - 1)
                verify(exactly = 1) { httpClient.getData() }
            }

        @Test
        fun `given a refresh is cancelled while it fetches, when the app returns to the foreground, then the loop fetches at once`() =
            runRefreshTest {
                completeLaunchFetch()
                var calls = 0
                every { httpClient.getData() } answers {
                    calls++
                    if (calls == 1) sourceConfigManager.stopPeriodicRefresh()
                    Result.Success(readFileAsString(downloadedSourceConfig))
                }
                advanceBy(interval)
                sourceConfigManager.startPeriodicRefresh()
                advanceBy(0)
                assertEquals(1, calls)
                verify(exactly = 1) { sourceConfigState.dispatch(any()) }

                advanceBy(1.minutes.inWholeMilliseconds)
                sourceConfigManager.startPeriodicRefresh()
                advanceBy(0)

                assertEquals(2, calls)
                verify(exactly = 2) { sourceConfigState.dispatch(any()) }
            }

        @Test
        fun `given the device is offline when a refresh is due, when the device comes online, then the loop fetches`() =
            runRefreshTest {
                completeLaunchFetch()
                connectivityFlow.value = false

                sourceConfigManager.startPeriodicRefresh()
                advanceBy(interval)
                verify(exactly = 1) { httpClient.getData() }

                simulateConnectionAvailability()
                verify(exactly = 2) { httpClient.getData() }
            }

        @OptIn(UseWithCaution::class)
        @Test
        fun `given a refresh gets a 400, when the loop continues, then the SDK keeps its config and fetches at the next interval`() =
            runRefreshTest {
                completeLaunchFetch()
                every { httpClient.getData() } returns Result.Failure(error = NetworkErrorStatus.Error400)

                sourceConfigManager.startPeriodicRefresh()
                advanceBy(interval)
                advanceBy(interval)

                verify(exactly = 3) { httpClient.getData() }
                coVerify(exactly = 0) { analytics.handleInvalidWriteKey() }
                coVerify(exactly = 0) { analytics.shutdown() }
                verify(exactly = 1) { sourceConfigState.dispatch(any()) }
            }

        @Test
        fun `given a refresh gets a 5xx, when the loop continues, then the SDK makes one attempt per interval with no backoff retries`() =
            runRefreshTest {
                completeLaunchFetch()
                every { httpClient.getData() } returns Result.Failure(error = NetworkErrorStatus.ErrorRetry(statusCode = 503))

                sourceConfigManager.startPeriodicRefresh()
                advanceBy(interval)
                verify(exactly = 2) { httpClient.getData() }

                advanceBy(interval - 1)
                verify(exactly = 2) { httpClient.getData() }

                advanceBy(1)
                verify(exactly = 3) { httpClient.getData() }
                verify(exactly = 1) { sourceConfigState.dispatch(any()) }
            }

        @Test
        fun `given a refresh gets an invalid body, when the refresh ends, then the SDK keeps its config`() =
            runRefreshTest {
                completeLaunchFetch()
                every { httpClient.getData() } returns Result.Success("not a source config")

                sourceConfigManager.startPeriodicRefresh()
                advanceBy(interval)

                verify(exactly = 2) { httpClient.getData() }
                verify(exactly = 1) { sourceConfigState.dispatch(any()) }
                coVerify(exactly = 1) { analytics.storage.write(StorageKeys.SOURCE_CONFIG_PAYLOAD, any<String>()) }
            }

        @Test
        fun `given the clock moved back after the last fetch, when the app comes to the foreground, then the loop fetches at once`() =
            runRefreshTest {
                completeLaunchFetch()
                clockOffsetMillis = 10.minutes.inWholeMilliseconds

                sourceConfigManager.startPeriodicRefresh()
                advanceBy(0)

                verify(exactly = 2) { httpClient.getData() }
            }

        @Test
        fun `given a running loop, when a test sets an interval below one minute, then the loop restarts with a one minute interval`() =
            runRefreshTest {
                completeLaunchFetch()
                sourceConfigManager.startPeriodicRefresh()
                advanceBy(0)

                sourceConfigManager.setRefreshIntervalForTesting(1.seconds)
                advanceBy(1.minutes.inWholeMilliseconds - 1)
                verify(exactly = 1) { httpClient.getData() }

                advanceBy(1)
                verify(exactly = 2) { httpClient.getData() }
            }

        // runTest drains the scheduler at the end, so the endless loop must stop inside the test body.
        private fun runRefreshTest(block: suspend TestScope.() -> Unit) = runTest(testDispatcher) {
            try {
                block()
            } finally {
                sourceConfigManager.stopPeriodicRefresh()
            }
        }

        private fun TestScope.completeLaunchFetch() {
            sourceConfigManager.fetchSourceConfigAtLaunchAndNotifyObservers()
            simulateConnectionAvailability()
            verify(exactly = 1) { httpClient.getData() }
        }

        private fun advanceBy(millis: Long) {
            testDispatcher.scheduler.advanceTimeBy(millis)
            testDispatcher.scheduler.runCurrent()
        }
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
