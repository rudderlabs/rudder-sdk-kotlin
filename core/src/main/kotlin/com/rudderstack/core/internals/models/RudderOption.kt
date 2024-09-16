package com.rudderstack.core.internals.models

/**
 * Represents options that can be passed with a message in the RudderStack analytics context.
 *
 * @property integrations A map of integration names to a boolean indicating if they are enabled.
 * @property externalIds A list of maps representing external IDs associated with the message.
 */
data class RudderOption(
    val integrations: Map<String, Boolean> = defaultIntegrations,
    val externalIds: List<Map<String, String>> = emptyList(),
)

private val defaultIntegrations by lazy {
    mapOf(
        "All" to true,
    )
}

internal fun Message.updateIntegrations() {
    when (this) {
        is TrackEvent -> {
            this.integrations = defaultIntegrations + options.integrations
        }

        else -> {
            // Do nothing
        }
    }
}
