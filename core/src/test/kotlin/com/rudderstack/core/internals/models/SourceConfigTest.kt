package com.rudderstack.core.internals.models

import com.rudderstack.core.internals.utils.LenientJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader

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

    @Test
    fun `given source config without destination, when source config is parsed, then source config object should be created`() {
        val jsonString = readFileAsString(sourceConfigWithoutDestination)

        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(jsonString)

        checkDefaultSourceConfigProperties(sourceConfig)
        checkMetricsSourceConfigProperties(sourceConfig)
        assertEquals(0, sourceConfig.source.destinations?.size)
    }

    @Test
    fun `given source config with single destination, when source config is parsed, then source config object should be created`() {
        val jsonString = readFileAsString(sourceConfigWithSingleDestination)

        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(jsonString)

        checkDefaultSourceConfigProperties(sourceConfig)
        checkMetricsSourceConfigProperties(sourceConfig)
        assertEquals(1, sourceConfig.source.destinations?.size)
        val destination = sourceConfig.source.destinations?.get(0)
        checkDestinationSourceConfigProperties(destination)
    }

    @Test
    fun `given source config with multiple destination, when source config is parsed, then source config object should be created`() {
        val jsonString = readFileAsString(sourceConfigWithMultipleDestination)

        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(jsonString)

        checkDefaultSourceConfigProperties(sourceConfig)
        checkMetricsSourceConfigProperties(sourceConfig)
        assertEquals(2, sourceConfig.source.destinations?.size)
        for (destination in sourceConfig.source.destinations ?: emptyList()) {
            checkDestinationSourceConfigProperties(destination)
        }
    }

    @Test
    fun `given source config without metrics config, when source config is parsed, then source config object should be created`() {
        val jsonString = readFileAsString(sourceConfigWithoutMetricsConfig)

        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(jsonString)

        checkDefaultSourceConfigProperties(sourceConfig)
        assertEquals(null, sourceConfig.source.metricConfig)
    }

    private fun checkDefaultSourceConfigProperties(sourceConfig: SourceConfig) {
        assertEquals(ID, sourceConfig.source.sourceId)
        assertEquals(NAME, sourceConfig.source.sourceName)
        assertEquals(WRITE_KEY, sourceConfig.source.writeKey)
        assertEquals(ENABLED, sourceConfig.source.isSourceEnabled)
        assertEquals(WORKSPACE_ID, sourceConfig.source.workspaceId)
        assertEquals(UPDATED_AT, sourceConfig.source.updatedAt)
    }

    private fun checkMetricsSourceConfigProperties(sourceConfig: SourceConfig) {
        assertEquals(ERROR_COLLECTION_ENABLED, sourceConfig.source.metricConfig?.statsCollection?.errors?.enabled)
        assertEquals(METRICS_COLLECTION_ENABLED, sourceConfig.source.metricConfig?.statsCollection?.metrics?.enabled)
    }

    private fun checkDestinationSourceConfigProperties(destination: Destination?) {
        assertTrue(destination?.destinationId?.isNotEmpty() ?: false)
        assertTrue(destination?.destinationName?.isNotEmpty() ?: false)
        assertTrue(destination?.isDestinationEnabled ?: false)
        assertTrue(destination?.destinationConfig?.isNotEmpty() ?: false)
        assertTrue(destination?.destinationDefinitionId?.isNotEmpty() ?: false)
        assertTrue(destination?.destinationDefinition?.name?.isNotEmpty() ?: false)
        assertTrue(destination?.destinationDefinition?.displayName?.isNotEmpty() ?: false)
        assertTrue(destination?.updatedAt?.isNotEmpty() ?: false)
        assertFalse(destination?.shouldApplyDeviceModeTransformation ?: true)
        assertFalse(destination?.propagateEventsUntransformedOnError ?: true)
    }

    private fun readFileAsString(fileName: String): String {
        val inputStream = this::class.java.classLoader.getResourceAsStream(fileName)
        return inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
    }
}

