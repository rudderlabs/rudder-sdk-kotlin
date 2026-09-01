package com.rudderstack.sdk.kotlin.core.internals.models.consent

import com.rudderstack.sdk.kotlin.core.internals.models.consent.ConsentManagementState.Companion.normalized
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val CONSENT_MANAGEMENT_KEY = "consentManagement"
private const val PROVIDER_KEY = "provider"
private const val CONSENTS_KEY = "consents"
private const val CONSENT_KEY = "consent"
private const val RESOLUTION_STRATEGY_KEY = "resolutionStrategy"

/**
 * Strategy for matching configured consent IDs against the allowed list.
 *
 * `and`/`all` require every configured ID; `or`/`any` require at least one.
 * Missing, empty, or unrecognized values normalize to [ALL].
 */
internal enum class ConsentResolutionStrategy {
    ALL,
    ANY;

    companion object {

        fun from(rawValue: String?): ConsentResolutionStrategy = when (rawValue?.trim()?.lowercase()) {
            "or", "any" -> ANY
            else -> ALL
        }
    }
}

/**
 * The single consent decision point shared by the initialization and event gates.
 *
 * A pure, stateless, fail-open resolution predicate: missing configuration,
 * malformed entries, and unknown strategies all resolve to consented — gating
 * must never break event delivery because of unexpected data. Only
 * `allowedConsentIds` participate in matching; `deniedConsentIds` are stamped
 * on events but never consulted.
 */
@InternalRudderApi
object ConsentResolver {

    /**
     * Resolves whether a destination is consented under the current consent [state].
     *
     * @param state The active consent state.
     * @param destinationConfig The destination's raw config, carrying optional `consentManagement` entries.
     * @return `true` when the destination may receive events.
     */
    fun resolve(state: ConsentManagementState, destinationConfig: JsonObject?): Boolean {
        // Rule 1: disabled -> consented.
        if (!state.enabled) return true

        // Rule 2: first entry matching the active provider; none (or no array) -> consented.
        val entries = destinationConfig?.get(CONSENT_MANAGEMENT_KEY).asObjectList() ?: emptyList()
        val entry = entries.firstOrNull { it.stringValue(PROVIDER_KEY) == state.provider.value } ?: return true

        // Rule 3: configured consent IDs, trimmed with empties dropped; empty -> consented.
        val configuredIds = (entry[CONSENTS_KEY].asObjectList() ?: emptyList()).mapNotNull { it.stringValue(CONSENT_KEY) }
        val cleanedIds = configuredIds.normalized()
        if (cleanedIds.isEmpty()) return true

        // Rules 4 & 5: normalize the strategy, match against the allowed IDs only.
        return when (ConsentResolutionStrategy.from(entry.stringValue(RESOLUTION_STRATEGY_KEY))) {
            ConsentResolutionStrategy.ALL -> cleanedIds.all { it in state.allowedConsentIds }
            ConsentResolutionStrategy.ANY -> cleanedIds.any { it in state.allowedConsentIds }
        }
    }

    // Any non-object element voids the whole list, keeping list handling wholesale rather than per-entry.
    private fun Any?.asObjectList(): List<JsonObject>? {
        val array = this as? JsonArray ?: return null
        val objects = array.filterIsInstance<JsonObject>()
        return objects.takeIf { it.size == array.size }
    }

    // Non-string primitives read as null instead of being coerced to text.
    private fun JsonObject.stringValue(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
}
