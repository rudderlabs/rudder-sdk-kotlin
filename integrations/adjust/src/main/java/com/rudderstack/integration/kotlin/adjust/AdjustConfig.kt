package com.rudderstack.integration.kotlin.adjust

import kotlinx.serialization.Serializable

/**
 * TODO
 * @param appToken
 * @param customMappings
 */
@Serializable
data class AdjustConfig(
    val appToken: String,
    val customMappings: List<CustomMapping>,
)

/**
 * TODO
 * @param from
 * @param to
 */
@Serializable
data class CustomMapping(
    val from: String,
    val to: String
)
