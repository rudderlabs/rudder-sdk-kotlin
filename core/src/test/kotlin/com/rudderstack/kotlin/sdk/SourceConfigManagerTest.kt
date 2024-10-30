package com.rudderstack.kotlin.sdk

import com.rudderstack.kotlin.sdk.internals.models.SourceConfig
import com.rudderstack.kotlin.sdk.internals.network.ErrorStatus
import com.rudderstack.kotlin.sdk.internals.network.HttpClient
import com.rudderstack.kotlin.sdk.internals.network.Result
import com.rudderstack.kotlin.sdk.internals.statemanagement.FlowState
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys
import com.rudderstack.kotlin.sdk.internals.utils.LenientJson
import com.rudderstack.kotlin.sdk.internals.utils.empty
import com.rudderstack.kotlin.sdk.internals.utils.mockAnalytics
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
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

private const val sourceConfig = "config/source_config_without_destination.json"
private const val anotherSourceConfig = "config/source_config_with_single_destination.json"

@OptIn(ExperimentalCoroutinesApi::class)
class SourceConfigManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val analytics: Analytics = mockAnalytics(testScope, testDispatcher)
    private val sourceConfigState: FlowState<SourceConfig> = spyk(FlowState(SourceConfig.initialState()))
    private val httpClient: HttpClient = mockk(relaxed = true)
    private lateinit var sourceConfigManager: SourceConfigManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sourceConfigManager = SourceConfigManager(analytics, sourceConfigState, httpClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given a stored source config, when fetchAndUpdateSourceConfig is called, then it should update the state`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfig)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)
            coEvery {
                analytics.configuration.storage.readString(
                    StorageKeys.SOURCE_CONFIG_PAYLOAD,
                    defaultVal = String.empty()
                )
            } returns sourceConfigString

            sourceConfigManager.fetchAndUpdateSourceConfig()

            coVerify {
                sourceConfigState.dispatch(match { it is SourceConfig.UpdateAction && it.updatedSourceConfig == sourceConfig })
            }
        }

    @Test
    fun `given a response on downloadSourceConfig, when the response is successful, then source config should be updated`() =
        runTest {
            val sourceConfigString = readFileAsString(sourceConfig)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)

            coEvery { httpClient.getData() } returns Result.Success(sourceConfigString)

            sourceConfigManager.fetchAndUpdateSourceConfig()

            coVerify {
                sourceConfigState.dispatch(match { it is SourceConfig.UpdateAction && it.updatedSourceConfig == sourceConfig })
            }
        }

    @Test
    fun `given sourceConfig fetched from storage and network, when fetchAndUpdateSourceConfig called, then update is called once`() = runTest {
        val sourceConfigNetwork = readFileAsString(sourceConfig)
        val sourceConfigStorage = readFileAsString(anotherSourceConfig)

        coEvery {
            analytics.configuration.storage.readString(
                StorageKeys.SOURCE_CONFIG_PAYLOAD,
                defaultVal = String.empty()
            )
        } returns sourceConfigStorage

        coEvery { httpClient.getData() } returns Result.Success(sourceConfigNetwork)

        sourceConfigManager.fetchAndUpdateSourceConfig()

        coVerify(exactly = 1) { sourceConfigState.dispatch(any()) }
    }

    @Test
    fun `given a response on downloadSourceConfig, when the response is not successful, then source config should not be updated`() =
        runTest {
            coEvery { httpClient.getData() } returns Result.Failure(
                error = Exception(),
                status = ErrorStatus.SERVER_ERROR
            )

            sourceConfigManager.fetchAndUpdateSourceConfig()

            coVerify(exactly = 0) { sourceConfigState.dispatch(any()) }
        }

    @Test
    fun `given a response on downloadSourceConfig, when an exception is thrown, then source config should not be updated`() =
        runTest {
            coEvery { httpClient.getData() } throws Exception()

            sourceConfigManager.fetchAndUpdateSourceConfig()

            coVerify(exactly = 0) { sourceConfigState.dispatch(any()) }
        }

    @Test
    fun `given source config is not stored, when fetchAndUpdateSourceConfig called, then source config should not be updated`() =
        runTest {
            coEvery {
                analytics.configuration.storage.readString(
                    StorageKeys.SOURCE_CONFIG_PAYLOAD,
                    defaultVal = String.empty()
                )
            } returns String.empty()

            sourceConfigManager.fetchAndUpdateSourceConfig()

            coVerify(exactly = 0) { sourceConfigState.dispatch(any()) }
        }
}
