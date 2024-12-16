package com.rudderstack.sdk.kotlin.core.internals.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Represents options that can be passed with a event in the RudderStack analytics context.
 *
 * @property integrations A JSON object representing integrations associated with the event.
 * @property externalIds A list of maps representing external IDs associated with the event.
 * @property customContext A JSON object representing custom contexts associated with the event.
 */
data class RudderOption(
    val integrations: JsonObject = emptyJsonObject,
    val externalIds: List<ExternalId> = emptyList(),
    val customContext: JsonObject = emptyJsonObject,
)

/**
 * Represents an external ID associated with a event.
 *
 * @property type The type of the external ID.
 * @property id The ID value.
 */
@Serializable
data class ExternalId(
    val type: String,
    val id: String,
)
