package com.rudderstack.core.internals.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Represents the configuration for a source in the RudderStack server.
 *
 * @property source The configuration details of a RudderStack source.
 */
@Serializable
data class SourceConfig(
    val source: RudderServerConfigSource
)

/**
 * Represents the configuration of a source from the RudderStack server.
 *
 * @property sourceId The unique identifier of the source.
 * @property sourceName The name of the source.
 * @property writeKey The write key associated with the source.
 * @property metricConfig The metrics configuration for the source, defaulting to a new instance of [MetricsConfig].
 * @property isSourceEnabled A flag indicating whether the source is enabled.
 * @property workspaceId The identifier of the workspace to which the source belongs.
 * @property destinations A list of destinations associated with this source, defaulting to an empty list.
 * @property updatedAt The timestamp of the last update to the source configuration.
 */
@Serializable
data class RudderServerConfigSource(
    @SerialName("id") val sourceId: String,
    @SerialName("name") val sourceName: String,
    val writeKey: String,
    @SerialName("config") val metricConfig: MetricsConfig = MetricsConfig(),
    @SerialName("enabled") val isSourceEnabled: Boolean,
    val workspaceId: String,
    val destinations: List<Destination> = emptyList(),
    val updatedAt: String,
)

/**
 * Represents the configuration for metrics collection.
 *
 * @property statsCollection The configuration for collecting statistics, defaulting to a new instance of [StatsCollection].
 */
@Serializable
data class MetricsConfig(
    val statsCollection: StatsCollection = StatsCollection()
)

/**
 * Represents the configuration for statistics collection related to errors and metrics.
 *
 * @property errors The configuration for error statistics collection, defaulting to a new instance of [Errors].
 * @property metrics The configuration for general metrics collection, defaulting to a new instance of [Metrics].
 */
@Serializable
data class StatsCollection(
    val errors: Errors = Errors(),
    val metrics: Metrics = Metrics(),
)

/**
 * Represents the configuration for error statistics collection.
 *
 * @property enabled A flag indicating whether error statistics collection is enabled, defaulting to false.
 */
@Serializable
data class Errors(
    val enabled: Boolean = false
)

/**
 * Represents the configuration for general metrics collection.
 *
 * @property enabled A flag indicating whether metrics collection is enabled, defaulting to false.
 */
@Serializable
data class Metrics(
    val enabled: Boolean = false
)

/**
 * Represents the configuration of a destination in RudderStack.
 *
 * @property destinationId The unique identifier of the destination.
 * @property destinationName The name of the destination.
 * @property isDestinationEnabled A flag indicating whether the destination is enabled.
 * @property destinationConfig The configuration settings for the destination, defaulting to an empty [JsonObject].
 * @property destinationDefinitionId The identifier of the destination definition.
 * @property destinationDefinition The definition details of the destination.
 * @property updatedAt The timestamp of the last update to the destination configuration.
 * @property shouldApplyDeviceModeTransformation A flag indicating whether device mode transformation should be applied.
 * @property propagateEventsUntransformedOnError A flag indicating whether to propagate events untransformed in case of an error.
 */
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

/**
 * Represents the definition of a destination in RudderStack.
 *
 * @property name The internal name of the destination.
 * @property displayName The display name of the destination.
 */
@Serializable
data class DestinationDefinition(
    val name: String,
    val displayName: String
)
