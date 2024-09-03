package com.rudderstack.core.internals.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SourceConfig(
    val source: RudderServerConfigSource
)

@Serializable
data class RudderServerConfigSource(
    @SerialName("id") val sourceId: String,
    @SerialName("name") val sourceName: String,
    val writeKey: String,
    @SerialName("config") val metricConfig: MetricsConfig? = null,
    @SerialName("enabled") val isSourceEnabled: Boolean,
    val workspaceId: String,
    val destinations: List<Destination>? = null,
    val updatedAt: String,
)

@Serializable
data class MetricsConfig(
    val statsCollection: StatsCollection
)

@Serializable
data class StatsCollection(
    val errors: Errors,
    val metrics: Metrics
)

@Serializable
data class Errors(
    val enabled: Boolean
)

@Serializable
data class Metrics(
    val enabled: Boolean
)

@Serializable
data class Destination(
    @SerialName("id") val destinationId: String,
    @SerialName("name") val destinationName: String,
    @SerialName("enabled") val isDestinationEnabled: Boolean,
    @SerialName("config") val destinationConfig: JsonObject = emptyJsonObject,
    val destinationDefinitionId: String,
    val destinationDefinition: DestinationDefinition,
    val updatedAt: String,
    val shouldApplyDeviceModeTransformation: Boolean,
    val propagateEventsUntransformedOnError: Boolean
)

@Serializable
data class DestinationDefinition(
    val name: String,
    val displayName: String
)
