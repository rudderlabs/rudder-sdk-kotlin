package com.rudderstack.sdk.kotlin.core.internals.models.consent

import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementConfiguration
import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementProvider
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.StateAction

/**
 * In-memory state holding the current consent values.
 *
 * [initialized] is `false` until consent values have actually been supplied (via configuration
 * or `setConsent`). While uninitialized, consent evaluation fails open — no destination is blocked.
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
         * Consent IDs are trimmed and empties dropped. The state is initialized only when
         * enabled and at least one non-empty list was supplied.
         */
        fun initialState(configuration: ConsentManagementConfiguration): ConsentManagementState {
            val allowed = configuration.allowedConsentIds.normalized()
            val denied = configuration.deniedConsentIds.normalized()
            return ConsentManagementState(
                enabled = configuration.enabled,
                provider = configuration.provider,
                allowedConsentIds = allowed,
                deniedConsentIds = denied,
                initialized = configuration.enabled && (allowed.isNotEmpty() || denied.isNotEmpty()),
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
