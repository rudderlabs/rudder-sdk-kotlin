package com.rudderstack.sdk.kotlin.core.internals.models.connectivity

import com.rudderstack.sdk.kotlin.core.internals.statemanagement.StateAction
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi

private const val DEFAULT_STATE = true
private const val CONNECTION_AVAILABLE = true
private const val CONNECTION_UNAVAILABLE = false

/**
 * State management for connectivity.
 */
@InternalRudderApi
class ConnectivityState {

    companion object {

        internal const val INITIAL_STATE = false
    }

    /**
     * Action to set the default state of connectivity.
     * Default state is always `true`.
     */
    class SetDefaultStateAction : StateAction<Boolean> {

        override fun reduce(currentState: Boolean): Boolean {
            return DEFAULT_STATE
        }
    }

    /**
     * Action to enable the connectivity.
     */
    class EnableConnectivityAction : StateAction<Boolean> {

        override fun reduce(currentState: Boolean): Boolean {
            return CONNECTION_AVAILABLE
        }
    }

    /**
     * Action to disable the connectivity.
     */
    class DisableConnectivityAction : StateAction<Boolean> {

        override fun reduce(currentState: Boolean): Boolean {
            return CONNECTION_UNAVAILABLE
        }
    }
}
