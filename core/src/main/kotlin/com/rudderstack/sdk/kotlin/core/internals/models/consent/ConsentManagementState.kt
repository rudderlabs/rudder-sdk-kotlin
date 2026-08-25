package com.rudderstack.sdk.kotlin.core.internals.models.consent

import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementConfiguration
import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementProvider
import com.rudderstack.sdk.kotlin.core.internals.models.SDKManagedContextKey
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.StateAction
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
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
 * The two consent ID lists are never both empty while [enabled] is `true`: a configuration
 * that enables consent management without supplying either list is a configuration error,
 * and the state is built inactive so the feature behaves as if it had never been enabled.
 *
 * @property enabled Whether consent management was enabled at load time.
 * @property provider The active consent provider.
 * @property allowedConsentIds The consent IDs the user has granted.
 * @property deniedConsentIds The consent IDs the user has denied.
 */
@InternalRudderApi
data class ConsentManagementState(
    val enabled: Boolean = false,
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
            val active = configuration.enabled && (allowed.isNotEmpty() || denied.isNotEmpty())
            return ConsentManagementState(
                enabled = active,
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
 * Builds the `consentManagement` context block for this state, wrapped under its key and
 * ready to merge into an event context. Shared by every stamp site so each produces an
 * identical payload.
 */
@InternalRudderApi
fun ConsentManagementState.toConsentContextBlock(): JsonObject = buildJsonObject {
    put(
        SDKManagedContextKey.CONSENT_MANAGEMENT.key,
        buildJsonObject {
            put(PROVIDER_KEY, provider.value)
            put(ALLOWED_CONSENT_IDS_KEY, buildJsonArray { allowedConsentIds.forEach { add(it) } })
            put(DENIED_CONSENT_IDS_KEY, buildJsonArray { deniedConsentIds.forEach { add(it) } })
        }
    )
}
