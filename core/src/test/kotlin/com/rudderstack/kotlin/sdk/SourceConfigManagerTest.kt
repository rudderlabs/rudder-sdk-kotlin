package com.rudderstack.kotlin.sdk

import com.rudderstack.kotlin.sdk.internals.models.SourceConfig
import com.rudderstack.kotlin.sdk.internals.network.ErrorStatus
import com.rudderstack.kotlin.sdk.internals.network.HttpClient
import com.rudderstack.kotlin.sdk.internals.utils.Result
import com.rudderstack.kotlin.sdk.internals.statemanagement.FlowState
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys
import com.rudderstack.kotlin.sdk.internals.utils.LenientJson
import com.rudderstack.kotlin.sdk.internals.utils.empty
import io.mockk.every
import io.mockk.mockk
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

private const val fetchedSourceConfig = "config/source_config_without_destination.json"
private const val downloadedSourceConfig = "config/source_config_with_single_destination.json"

@OptIn(ExperimentalCoroutinesApi::class)
class SourceConfigManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val analytics: Analytics = mockAnalytics(testScope, testDispatcher)
    private val sourceConfigState: FlowState<SourceConfig> = mockk(relaxed = true)
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
    fun `given a stored source config and downloadSourceConfig fails, when fetchAndUpdateSourceConfig is called, then it should update the state`() =
        runTest {
            val sourceConfigString = readFileAsString(fetchedSourceConfig)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)
            every {
                analytics.configuration.storage.readString(
                    StorageKeys.SOURCE_CONFIG_PAYLOAD,
                    defaultVal = String.empty()
                )
            } returns sourceConfigString
            every { httpClient.getData() } returns Result.Failure(
                error = Exception(),
                status = ErrorStatus.SERVER_ERROR
            )

            sourceConfigManager.fetchAndUpdateSourceConfig()

            verify(exactly = 1) {
                sourceConfigState.dispatch(match { it is SourceConfig.UpdateAction && it.updatedSourceConfig == sourceConfig })
            }
        }

    @Test
    fun `given a response on downloadSourceConfig, when fetchAndUpdateSourceConfig called, then source config should be updated`() =
        runTest {
            val downloadedSourceConfigString = readFileAsString(downloadedSourceConfig)
            val downloadedSourceConfig = LenientJson.decodeFromString<SourceConfig>(downloadedSourceConfigString)

            val fetchedSourceConfigString = readFileAsString(fetchedSourceConfig)

            every { httpClient.getData() } returns Result.Success(downloadedSourceConfigString)
            every {
                analytics.configuration.storage.readString(
                    StorageKeys.SOURCE_CONFIG_PAYLOAD,
                    defaultVal = String.empty()
                )
            } returns fetchedSourceConfigString

            sourceConfigManager.fetchAndUpdateSourceConfig()

            verify(exactly = 1) {
                sourceConfigState.dispatch(match { it is SourceConfig.UpdateAction && it.updatedSourceConfig == downloadedSourceConfig })
            }
        }

    @Test
    fun `given a failed response on downloadSourceConfig and no stored sourceConfig, when fetchAndUpdateSourceConfig called, then source config should not be updated`() =
        runTest {
            every { httpClient.getData() } returns Result.Failure(
                error = Exception(),
                status = ErrorStatus.SERVER_ERROR
            )
            every {
                analytics.configuration.storage.readString(
                    StorageKeys.SOURCE_CONFIG_PAYLOAD,
                    defaultVal = String.empty()
                )
            } returns String.empty()

            sourceConfigManager.fetchAndUpdateSourceConfig()

            verify(exactly = 0) { sourceConfigState.dispatch(any()) }
        }

    @Test
    fun `given an exception on downloadSourceConfig and no stored source config, when fetchAndUpdateSourceConfig called, then source config should not be updated`() =
        runTest {
            every { httpClient.getData() } throws Exception()
            every {
                analytics.configuration.storage.readString(
                    StorageKeys.SOURCE_CONFIG_PAYLOAD,
                    defaultVal = String.empty()
                )
            } returns String.empty()

            sourceConfigManager.fetchAndUpdateSourceConfig()

            verify(exactly = 0) { sourceConfigState.dispatch(any()) }
        }

    @Test
    fun `given a stored source config and downloadSourceConfig throws exception, when fetchAndUpdateSourceConfig called, then it should update the state`() =
        runTest {
            val sourceConfigString = readFileAsString(fetchedSourceConfig)
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)
            every {
                analytics.configuration.storage.readString(
                    StorageKeys.SOURCE_CONFIG_PAYLOAD,
                    defaultVal = String.empty()
                )
            } returns sourceConfigString
            every { httpClient.getData() } throws Exception()

            sourceConfigManager.fetchAndUpdateSourceConfig()

            verify(exactly = 1) {
                sourceConfigState.dispatch(match { it is SourceConfig.UpdateAction && it.updatedSourceConfig == sourceConfig })
            }
        }
}
