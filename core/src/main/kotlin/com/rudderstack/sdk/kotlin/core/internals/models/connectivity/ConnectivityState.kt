package com.rudderstack.sdk.kotlin.core.internals.models.connectivity

import com.rudderstack.sdk.kotlin.core.internals.statemanagement.FlowAction
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi

private const val DEFAULT_STATE = true

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
    class SetDefaultStateAction : FlowAction<Boolean> {

        override fun reduce(currentState: Boolean): Boolean {
            return DEFAULT_STATE
        }
    }

    /**
     * Action to toggle the current state of connectivity.
     *
     * @param newState The new state of connectivity.
     */
    class ToggleStateAction(private val newState: Boolean) : FlowAction<Boolean> {

        override fun reduce(currentState: Boolean): Boolean {
            return newState
        }
    }
}
