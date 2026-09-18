package com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils

import com.rudderstack.sdk.kotlin.android.models.consent.ConsentManagementState
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.StateAction

/**
 * Replaces the consent state wholesale, including [ConsentManagementState.active].
 *
 * `SetConsentAction` cannot do this: `active` is a load-time setting, so the production reducer
 * returns the current state untouched while consent is inactive. Tests that need to simulate a
 * consent change from an inactive start dispatch this instead.
 */
internal class ReplaceConsentStateAction(
    private val newState: ConsentManagementState
) : StateAction<ConsentManagementState> {

    override fun reduce(currentState: ConsentManagementState): ConsentManagementState = newState
}
