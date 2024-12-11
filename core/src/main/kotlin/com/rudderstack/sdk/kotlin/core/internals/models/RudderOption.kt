package com.rudderstack.sdk.kotlin.core.internals.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Represents options that can be passed with a message in the RudderStack analytics context.
 *
 * @property integrations A JSON object representing integrations associated with the message.
 * @property externalIds A list of maps representing external IDs associated with the message.
 * @property customContext A JSON object representing custom contexts associated with the message.
 */
data class RudderOption(
    val integrations: JsonObject = emptyJsonObject,
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
