package com.rudderstack.kotlin.sdk

import com.rudderstack.kotlin.sdk.internals.models.SourceConfig
import com.rudderstack.kotlin.sdk.internals.network.ErrorStatus
import com.rudderstack.kotlin.sdk.internals.network.HttpClient
import com.rudderstack.kotlin.sdk.internals.network.Result
import com.rudderstack.kotlin.sdk.internals.statemanagement.SingleThreadStore
import com.rudderstack.kotlin.sdk.internals.utils.LenientJson
import com.rudderstack.kotlin.sdk.state.SourceConfigState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any

private const val sourceConfigSuccess = "config/source_config_without_destination.json"

@OptIn(ExperimentalCoroutinesApi::class)
class SourceConfigManagerTest {

    private val analytics: Analytics = mockk(relaxed = true)
    private val store: SingleThreadStore<SourceConfigState, SourceConfigState.UpdateAction> = mockk(relaxed = true)
    private val httpClient: HttpClient = mockk()
    private lateinit var sourceConfigManager: SourceConfigManager

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { analytics.networkDispatcher } returns testDispatcher
        sourceConfigManager = SourceConfigManager(analytics, store, httpClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given a response on fetchSourceConfig, when the response is successful, then source config store should be subscribed`() =
        runTest {
            val jsonString = readFileAsString(sourceConfigSuccess)

            coEvery { httpClient.getData() } returns Result.Success(jsonString)

            sourceConfigManager.fetchSourceConfig()

            coVerify { store.subscribe(any()) }
        }

    @Test
    fun `given a response on fetchSourceConfig, when the response is successful, then source config should be logged`() =
        runTest {
            val jsonString = readFileAsString(sourceConfigSuccess)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(jsonString)

            coEvery { httpClient.getData() } returns Result.Success(jsonString)

            sourceConfigManager.fetchSourceConfig()

            verify { analytics.configuration.logger.debug(log = "SourceConfig: $sourceConfig") }
        }

    @Test
    fun `given a response on fetchSourceConfig, when the response is not successful, then source config store should not be subscribed`() =
        runTest {
            coEvery { httpClient.getData() } returns Result.Failure(
                error = Exception(any<String>()),
                status = ErrorStatus.SERVER_ERROR
            )

            sourceConfigManager.fetchSourceConfig()

            coVerify(exactly = 0) { store.subscribe(any()) } // Ensure storeSourceConfig is not called
        }

    @Test
    fun `given a response on fetchSourceConfig, when the response is not successful, then an error should be logged`() =
        runTest {
            coEvery { httpClient.getData() } returns Result.Failure(
                error = Exception(any<String>()),
                status = ErrorStatus.SERVER_ERROR
            )

            sourceConfigManager.fetchSourceConfig()

            verify { analytics.configuration.logger.error(any(), any()) }
        }

    @Test
    fun `given a response on fetchSourceConfig, when a exception is thrown, then source config store should not be subscribed`() =
        runTest {
            coEvery { httpClient.getData() } returns Result.Failure(
                error = Exception(any<String>()),
                status = ErrorStatus.SERVER_ERROR
            )

            sourceConfigManager.fetchSourceConfig()

            coVerify(exactly = 0) { store.subscribe(any()) }
            verify { analytics.configuration.logger.error(any(), any()) }
        }
}

