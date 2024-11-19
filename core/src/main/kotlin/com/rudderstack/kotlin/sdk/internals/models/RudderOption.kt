package com.rudderstack.kotlin.sdk.internals.models

import com.rudderstack.kotlin.sdk.internals.utils.DEFAULT_INTEGRATIONS
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Represents options that can be passed with a message in the RudderStack analytics context.
 *
 * @property integrations A map of integration names to a boolean indicating if they are enabled.
 * @property externalIds A list of maps representing external IDs associated with the message.
 * @property customContext A JSON object representing custom contexts associated with the message.
 */
data class RudderOption(
    val integrations: Map<String, Boolean> = DEFAULT_INTEGRATIONS,
    val externalIds: List<ExternalId> = emptyList(),
    val customContext: JsonObject = emptyJsonObject,
)

/**
 * Represents an external ID associated with a message.
 *
 * @property type The type of the external ID.
 * @property id The ID value.
 */
@Serializable
data class ExternalId(
    val type: String,
    val id: String,
)
