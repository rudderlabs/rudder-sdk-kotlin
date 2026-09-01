package com.rudderstack.sdk.kotlin.core.internals.models.consent

import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementOptions
import com.rudderstack.sdk.kotlin.core.internals.models.consent.ConsentManagementState.Companion.normalized

/**
 * Replaces the consent lists in [ConsentManagementState].
 *
 * This is a full replacement, not a merge: the supplied lists overwrite both existing lists.
 * An update carrying no consent IDs at all is rejected — the current state is returned
 * unchanged. [ConsentManagementState.enabled] and [ConsentManagementState.provider] are
 * load-time settings and are never modified at runtime.
 */
internal class SetConsentAction(
    private val options: ConsentManagementOptions,
) : ConsentManagementState.ConsentManagementStateAction {

    override fun reduce(currentState: ConsentManagementState): ConsentManagementState {
        if (!currentState.enabled) return currentState

        val allowed = options.allowedConsentIds.normalized()
        val denied = options.deniedConsentIds.normalized()
        if (allowed.isEmpty() && denied.isEmpty()) return currentState

        return currentState.copy(
            allowedConsentIds = allowed,
            deniedConsentIds = denied,
        )
    }
}
