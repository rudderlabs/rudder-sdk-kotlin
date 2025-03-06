package com.rudderstack.sdk.kotlin.core.internals.statemanagement

import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * A [State] is a wrapper around [MutableStateFlow] that can be updated using [FlowAction]s.
 *
 * It provides a [dispatch] method to update the state based on the given [FlowAction].
 */
interface State<T> : MutableStateFlow<T> {

    /**
     * Dispatches the given [action] to update the state.
     */
    fun dispatch(action: FlowAction<T>)
}

private class StateImpl<T>(initialState: T) : State<T>, MutableStateFlow<T> by MutableStateFlow(initialState) {

    override fun dispatch(action: FlowAction<T>) {
        this.update { currentValue ->
            action.reduce(currentValue)
        }
    }
}

/**
 * Creates a [State] with the given initial [initialState].
 */
@InternalRudderApi
fun <T> State(initialState: T): State<T> {
    return StateImpl(initialState)
}
