package com.rudderstack.integration.kotlin.adjust

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This class represents the configuration for the Adjust Integration.
 *
 * @param appToken The app token required for the Adjust SDK.
 * @param eventToTokenMappings The list of mappings from event to token.
 */
@Serializable
data class AdjustConfig(
    val appToken: String,
    @SerialName("customMappings") val eventToTokenMappings: List<EventToTokenMapping>,
)

/**
 * This class represents the mapping of an event to a token.
 *
 * @param event The event name.
 * @param token The token corresponding to the event.
 */
@Serializable
data class EventToTokenMapping(
    @SerialName("from") val event: String,
    @SerialName("to") val token: String
)
