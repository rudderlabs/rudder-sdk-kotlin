package com.rudderstack.sdk.kotlin.android.models.consent

import com.rudderstack.sdk.kotlin.android.consent.ConsentManagementConfiguration
import com.rudderstack.sdk.kotlin.android.consent.ConsentManagementProvider
import com.rudderstack.sdk.kotlin.core.internals.models.SDKManagedContextKey
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.StateAction
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val PROVIDER_KEY = "provider"
private const val ALLOWED_CONSENT_IDS_KEY = "allowedConsentIds"
private const val DENIED_CONSENT_IDS_KEY = "deniedConsentIds"

/**
 * In-memory state holding the current consent values.
 *
 * The two consent ID lists are never both empty while [active] is `true`: a configuration
 * that enables consent management without supplying either list is a configuration error,
 * and the state is built inactive so the feature behaves as if it had never been enabled.
 *
 * @property active Whether consent management is in force: enabled at load time with at
 *                  least one consent ID supplied.
 * @property provider The active consent provider.
 * @property allowedConsentIds The consent IDs the user has granted.
 * @property deniedConsentIds The consent IDs the user has denied.
 */
internal data class ConsentManagementState(
    val active: Boolean = false,
    val provider: ConsentManagementProvider = ConsentManagementProvider.CUSTOM,
    val allowedConsentIds: List<String> = emptyList(),
    val deniedConsentIds: List<String> = emptyList(),
) {

    companion object {

        /**
         * Builds the initial consent state from the load-time [configuration].
         *
         * Consent IDs are trimmed and empties dropped. Enabling consent management without
         * supplying either list is a configuration error: the state is built inactive, so the
         * feature behaves exactly as if it had never been enabled.
         */
        fun initialState(configuration: ConsentManagementConfiguration): ConsentManagementState {
            val allowed = configuration.allowedConsentIds.normalized()
            val denied = configuration.deniedConsentIds.normalized()
            return ConsentManagementState(
                active = configuration.enabled && (allowed.isNotEmpty() || denied.isNotEmpty()),
                provider = configuration.provider,
                allowedConsentIds = allowed,
                deniedConsentIds = denied,
            )
        }

        /**
         * Trims whitespace from each consent ID and drops the resulting empties.
         */
        internal fun List<String>.normalized(): List<String> = map { it.trim() }.filter { it.isNotEmpty() }
    }

    /**
     * A [StateAction] operating on [ConsentManagementState].
     */
    internal interface ConsentManagementStateAction : StateAction<ConsentManagementState>
}

/**
 * The inner `consentManagement` block for this state - provider plus both id lists.
 *
 * Deliberately unwrapped: this is the value the terminal guard compares against and re-asserts.
 */
internal val ConsentManagementState.consentStamp: JsonObject
    get() = buildJsonObject {
        put(PROVIDER_KEY, provider.value)
        put(ALLOWED_CONSENT_IDS_KEY, buildJsonArray { allowedConsentIds.forEach { add(it) } })
        put(DENIED_CONSENT_IDS_KEY, buildJsonArray { deniedConsentIds.forEach { add(it) } })
    }

/**
 * Builds the `consentManagement` context block for this state, wrapped under its key and
 * ready to merge into an event context. Shared by every stamp site so each produces an
 * identical payload.
 */
internal fun ConsentManagementState.toConsentContextBlock(): JsonObject = buildJsonObject {
    put(SDKManagedContextKey.CONSENT_MANAGEMENT.key, consentStamp)
}
