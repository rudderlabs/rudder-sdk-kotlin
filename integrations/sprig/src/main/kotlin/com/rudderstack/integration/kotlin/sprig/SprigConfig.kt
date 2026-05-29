package com.rudderstack.integration.kotlin.sprig

import kotlinx.serialization.Serializable

/**
 * This class represents the configuration for the Sprig Integration.
 *
 * @param environmentId The environment ID required for the Sprig SDK.
 */
@Serializable
data class SprigConfig(
    val environmentId: String,
)
