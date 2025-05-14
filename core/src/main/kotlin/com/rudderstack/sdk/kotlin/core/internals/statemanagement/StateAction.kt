package com.rudderstack.sdk.kotlin.core.internals.statemanagement

/**
 * A [StateAction] is a function interface that takes the current state and returns the new state.
 */
fun interface StateAction<T> {

    /**
     * Reduces the current state to a new state.
     */
    fun reduce(currentState: T): T
}
