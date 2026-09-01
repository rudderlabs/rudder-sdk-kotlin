package com.rudderstack.sdk.kotlin.core.internals.models.consent

import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementConfiguration
import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementProvider
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.StateAction

/**
 * In-memory state holding the current consent values.
 *
 * The two consent ID lists are never both empty while [enabled] is `true`: a configuration
 * that enables consent management without supplying either list is a configuration error,
 * and the state is built inactive so the feature behaves as if it had never been enabled.
 */
internal data class ConsentManagementState(
    val enabled: Boolean = false,
    val provider: ConsentManagementProvider = ConsentManagementProvider.CUSTOM,
    val allowedConsentIds: List<String> = emptyList(),
    val deniedConsentIds: List<String> = emptyList(),
    val initialized: Boolean = false,
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
                initialized = active,
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
