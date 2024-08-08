package com.rudderstack.core.internals.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SourceConfig(
    val source: RudderServerConfigSource? = null,
    val updatedAt: String? = null,
    val consentManagementMetadata: ConsentManagementMetadata? = null
) {
    @Serializable
    data class ConsentManagementMetadata(
        val providers: List<Provider>
    ) {
        @Serializable
        data class Provider(
            val provider: String,
            val resolutionStrategy: String
        )
    }
}

@Serializable
data class RudderServerConfigSource(
    @SerialName("id") val sourceId: String,
    @SerialName("name") val sourceName: String,
    val writeKey: String,
    @SerialName("config") val config: Config,
    @SerialName("enabled") val isSourceEnabled: Boolean,
    val workspaceId: String,
    val destinations: List<String>,
    val updatedAt: String
) {
    @Serializable
    data class Config(
        val statsCollection: StatsCollection
    ) {
        @Serializable
        data class StatsCollection(
            val errors: Errors,
            val metrics: Metrics
        ) {
            @Serializable
            data class Errors(
                val enabled: Boolean
            )

            @Serializable
            data class Metrics(
                val enabled: Boolean
            )
        }
    }
}
