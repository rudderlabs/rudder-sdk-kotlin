package com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils

import com.rudderstack.sdk.kotlin.core.internals.models.consent.ConsentManagementState
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.StateAction

/**
 * Replaces the consent state wholesale. The android module cannot reach the core-internal
 * `SetConsentAction`, so tests that need to move consent state dispatch this instead.
 */
internal class ReplaceConsentStateAction(
    private val newState: ConsentManagementState
) : StateAction<ConsentManagementState> {

    override fun reduce(currentState: ConsentManagementState): ConsentManagementState = newState
}
