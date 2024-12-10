package com.rudderstack.kotlin.core.internals.statemanagement

/**
 * A [FlowAction] is a function interface that takes the current state and returns the new state.
 */
fun interface FlowAction<T> {

    /**
     * Reduces the current state to a new state.
     */
    fun reduce(currentState: T): T
}
