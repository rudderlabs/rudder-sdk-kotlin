package com.rudderstack.sdk.kotlin.core.internals.models

import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig.Companion.serializer
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.FlowState
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.readFileAsString
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val ID = "<SOURCE_ID>"
private const val NAME = "Android"
private const val WRITE_KEY = "<WRITE_KEY>"
private const val ENABLED = true
private const val WORKSPACE_ID = "<WORKSPACE_ID>"
private const val UPDATED_AT = "2024-08-28T12:53:34.870Z"

private const val ERROR_COLLECTION_ENABLED = true
private const val METRICS_COLLECTION_ENABLED = false

private const val sourceConfigWithoutDestination = "config/source_config_without_destination.json"
private const val sourceConfigWithSingleDestination = "config/source_config_with_single_destination.json"
private const val sourceConfigWithMultipleDestination = "config/source_config_with_multiple_destination.json"
private const val sourceConfigWithoutMetricsConfig = "config/source_config_without_metrics_config.json"

class SourceConfigTest {

    @MockK
    private lateinit var mockStorage: Storage

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `given source config without destination, when source config is parsed, then source config object should be created`() {
        val jsonString = readFileAsString(sourceConfigWithoutDestination)

        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(jsonString)

        assertEquals(provideSourceConfigWithoutDestination(), sourceConfig)
    }

    @Test
    fun `given source config with single destination, when source config is parsed, then source config object should be created`() {
        val jsonString = readFileAsString(sourceConfigWithSingleDestination)

        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(jsonString)

        assertEquals(provideSourceConfigWithOneDestination(), sourceConfig)
    }

    @Test
    fun `given source config with multiple destinations, when source config is parsed, then source config object should be created`() {
        val jsonString = readFileAsString(sourceConfigWithMultipleDestination)

        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(jsonString)

        assertEquals(provideSourceConfigWithMultipleDestinations(), sourceConfig)
    }

    @Test
    fun `given source config without metrics config, when source config is parsed, then source config object should be created`() {
        val jsonString = readFileAsString(sourceConfigWithoutMetricsConfig)

        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(jsonString)

        assertEquals(provideServerConfigWithoutMetrics(), sourceConfig)
    }

    @Test
    fun `when initialState called, then it should return correct empty initial sourceConfig state`() {
        val initialState = SourceConfig.initialState()
        val expectedInitialState = SourceConfig(
            source = RudderServerConfigSource(
                sourceId = String.empty(),
                sourceName = String.empty(),
                writeKey = String.empty(),
                isSourceEnabled = false,
                workspaceId = String.empty(),
                updatedAt = String.empty()
            )
        )

        assertEquals(expectedInitialState, initialState)
    }

    @Test
    fun `given an initial SourceConfig state, when UpdateAction called, then sourceConfig state is updated`() {
        val sourceConfigFlowState = FlowState(SourceConfig.initialState())
        val newSourceConfig = SourceConfig(
            source = RudderServerConfigSource(
                sourceId = "newId",
                sourceName = "newName",
                writeKey = "newKey",
                isSourceEnabled = true,
                workspaceId = "newWorkspaceId",
                updatedAt = "2023-09-12"
            )
        )

        sourceConfigFlowState.dispatch(SourceConfig.UpdateAction(newSourceConfig))

        assertEquals(newSourceConfig, sourceConfigFlowState.value)
    }

    @Test
    fun `given a sourceConfig, when storeSourceConfig called, then it should store sourceConfig correctly`() = runTest {
        val sourceConfig = SourceConfig(
            source = RudderServerConfigSource(
                sourceId = "newId",
                sourceName = "newName",
                writeKey = "newKey",
                isSourceEnabled = true,
                workspaceId = "newWorkspaceId",
                updatedAt = "2023-09-12"
            )
        )

        sourceConfig.storeSourceConfig(mockStorage)

        coVerify { mockStorage.write(StorageKeys.SOURCE_CONFIG_PAYLOAD, Json.encodeToString(serializer(), sourceConfig)) }
    }
}

private fun provideSourceConfigWithoutDestination() = provideSourceConfig(
    source = provideRudderServerConfigSource(
        sourceId = ID,
        sourceName = NAME,
        writeKey = WRITE_KEY,
        isSourceEnabled = ENABLED,
        workspaceId = WORKSPACE_ID,
        updatedAt = UPDATED_AT,
        metricConfig = provideMetricsConfig(
            statsCollection = provideStatsCollection(
                errors = provideErrors(
                    enabled = ERROR_COLLECTION_ENABLED
                ),
                metrics = provideMetrics(
                    enabled = METRICS_COLLECTION_ENABLED
                )
            ),
        ),
    )
)

private fun provideSourceConfigWithOneDestination() = provideSourceConfig(
    source = provideRudderServerConfigSource(
        sourceId = ID,
        sourceName = NAME,
        writeKey = WRITE_KEY,
        isSourceEnabled = ENABLED,
        workspaceId = WORKSPACE_ID,
        updatedAt = UPDATED_AT,
        destinations = listOf(
            provideDestination(
                destinationName = "Braze Android",
                destinationDefinition = provideDestinationDefinition(
                    name = "BRAZE",
                    displayName = "Braze"
                ),
                updatedAt = UPDATED_AT,
            ),
        ),
        metricConfig = provideMetricsConfig(
            statsCollection = provideStatsCollection(
                errors = provideErrors(
                    enabled = ERROR_COLLECTION_ENABLED
                ),
                metrics = provideMetrics(
                    enabled = METRICS_COLLECTION_ENABLED
                )
            ),
        ),
    )
)

private fun provideSourceConfigWithMultipleDestinations() = provideSourceConfig(
    source = provideRudderServerConfigSource(
        sourceId = ID,
        sourceName = NAME,
        writeKey = WRITE_KEY,
        isSourceEnabled = ENABLED,
        workspaceId = WORKSPACE_ID,
        updatedAt = UPDATED_AT,
        destinations = listOf(
            provideDestination(
                destinationName = "FullStory Android",
                destinationDefinition = provideDestinationDefinition(
                    name = "FULLSTORY",
                    displayName = "Fullstory"
                ),
                updatedAt = UPDATED_AT,
            ),
            provideDestination(
                destinationName = "Braze Android",
                destinationDefinition = provideDestinationDefinition(
                    name = "BRAZE",
                    displayName = "Braze"
                ),
                updatedAt = UPDATED_AT,
            ),
        ),
        metricConfig = provideMetricsConfig(
            statsCollection = provideStatsCollection(
                errors = provideErrors(
                    enabled = ERROR_COLLECTION_ENABLED
                ),
                metrics = provideMetrics(
                    enabled = METRICS_COLLECTION_ENABLED
                )
            ),
        ),
    )
)

private fun provideServerConfigWithoutMetrics() = provideSourceConfig(
    source = provideRudderServerConfigSource(
        sourceId = ID,
        sourceName = NAME,
        writeKey = WRITE_KEY,
        isSourceEnabled = ENABLED,
        workspaceId = WORKSPACE_ID,
        updatedAt = UPDATED_AT,
    )
)
