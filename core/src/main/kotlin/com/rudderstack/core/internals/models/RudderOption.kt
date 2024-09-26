package com.rudderstack.core.internals.models

import com.rudderstack.core.internals.utils.mergeWithHigherPriorityTo
import kotlinx.serialization.json.JsonObject

/**
 * Represents options that can be passed with a message in the RudderStack analytics context.
 *
 * @property integrations A map of integration names to a boolean indicating if they are enabled.
 * @property externalIds A list of maps representing external IDs associated with the message.
 * @property customContexts A JSON object representing custom contexts associated with the message.
 */
data class RudderOption(
    val integrations: Map<String, Boolean> = defaultIntegrations,
    val externalIds: List<Map<String, String>> = emptyList(),
    val customContexts: JsonObject = emptyJsonObject,
)

private val defaultIntegrations by lazy {
    mapOf(
        "All" to true,
    )
}

internal fun Message.updateOption() {
    when (this) {
        is TrackEvent, is ScreenEvent -> {
            this.integrations = defaultIntegrations mergeWithHigherPriorityTo options.integrations
            this.context = options.customContexts mergeWithHigherPriorityTo context
        }

        else -> {
            // Do nothing
        }
    }
}
