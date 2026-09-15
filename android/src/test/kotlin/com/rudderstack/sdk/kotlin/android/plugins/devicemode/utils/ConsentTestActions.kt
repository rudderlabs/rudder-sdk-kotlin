package com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils

import com.rudderstack.sdk.kotlin.android.models.consent.ConsentManagementState
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.StateAction

/**
 * Replaces the consent state wholesale, including [ConsentManagementState.enabled].
 *
 * `SetConsentAction` cannot do this: `enabled` is a load-time setting, so the production reducer
 * returns the current state untouched while consent is disabled. Tests that need to simulate a
 * consent change from a disabled start dispatch this instead.
 */
internal class ReplaceConsentStateAction(
    private val newState: ConsentManagementState
) : StateAction<ConsentManagementState> {

    override fun reduce(currentState: ConsentManagementState): ConsentManagementState = newState
}
