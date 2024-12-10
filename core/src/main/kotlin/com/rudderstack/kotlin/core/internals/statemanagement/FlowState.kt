package com.rudderstack.kotlin.core.internals.statemanagement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * A [FlowState] is a wrapper around [MutableStateFlow] that can be updated using [FlowAction]s.
 *
 * It provides a [dispatch] method to update the state based on the given [FlowAction].
 */
interface FlowState<T> : MutableStateFlow<T> {

    /**
     * Dispatches the given [action] to update the state.
     */
    fun dispatch(action: FlowAction<T>)
}

private class FlowStateImpl<T>(initialState: T) : FlowState<T>, MutableStateFlow<T> by MutableStateFlow(initialState) {

    override fun dispatch(action: FlowAction<T>) {
        this.update { currentValue ->
            action.reduce(currentValue)
        }
    }
}

/**
 * Creates a [FlowState] with the given initial [initialState].
 */
fun <T> FlowState(initialState: T): FlowState<T> {
    return FlowStateImpl(initialState)
}
