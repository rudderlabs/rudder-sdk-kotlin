package com.rudderstack.sdk.kotlin.android.models.consent

import com.rudderstack.sdk.kotlin.android.consent.ConsentManagementOptions
import com.rudderstack.sdk.kotlin.android.models.consent.ConsentManagementState.Companion.normalized

/**
 * Replaces the consent lists in [ConsentManagementState].
 *
 * This is a full replacement, not a merge: the supplied lists overwrite both existing lists.
 * An inactive state never takes a runtime update. [ConsentManagementState.active] and
 * [ConsentManagementState.provider] are load-time settings and are never modified at runtime.
 *
 * Validating the update itself belongs to `Analytics.setConsent`, which refuses one carrying no
 * consent IDs at all and warns. Repeating that check here would leave two copies of one rule free
 * to drift apart.
 */
internal class SetConsentAction(
    private val options: ConsentManagementOptions,
) : ConsentManagementState.ConsentManagementStateAction {

    override fun reduce(currentState: ConsentManagementState): ConsentManagementState {
        // The feature's master switch, not a repeat of setConsent's validation: while consent
        // management is off the state declines the update outright, rather than copying itself.
        if (!currentState.active) return currentState

        return currentState.copy(
            allowedConsentIds = options.allowedConsentIds.normalized(),
            deniedConsentIds = options.deniedConsentIds.normalized(),
        )
    }
}
