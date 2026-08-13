package com.rudderstack.sdk.kotlin.core.internals.models.consent

import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementOptions
import com.rudderstack.sdk.kotlin.core.internals.models.consent.ConsentManagementState.Companion.normalized

/**
 * Replaces the consent lists in [ConsentManagementState].
 *
 * This is a full replacement, not a merge: the supplied lists overwrite both existing lists.
 * Empty options clear everything and revert the state to uninitialized (fail-open), matching
 * the JS SDK. [ConsentManagementState.enabled] and [ConsentManagementState.provider] are
 * load-time settings and are never modified at runtime.
 */
internal class SetConsentAction(
    private val options: ConsentManagementOptions,
) : ConsentManagementState.ConsentManagementStateAction {

    override fun reduce(currentState: ConsentManagementState): ConsentManagementState {
        val allowed = options.allowedConsentIds.normalized()
        val denied = options.deniedConsentIds.normalized()
        return currentState.copy(
            allowedConsentIds = allowed,
            deniedConsentIds = denied,
            initialized = allowed.isNotEmpty() || denied.isNotEmpty(),
        )
    }
}
