package com.rudderstack.integration.kotlin.adjust

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * TODO
 * @param appToken
 * @param eventToTokenMappings
 */
@Serializable
data class AdjustConfig(
    val appToken: String,
    @SerialName("customMappings") val eventToTokenMappings: List<EventToTokenMapping>,
)

/**
 * TODO
 * @param event
 * @param token
 */
@Serializable
data class EventToTokenMapping(
    @SerialName("from") val event: String,
    @SerialName("to") val token: String
)
