@file:Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")

package com.rudderstack.kotlin.sdk.internals.statemanagement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

interface FlowState<T> : MutableStateFlow<T> {
    fun dispatch(action: FlowAction<T>)
}

private class FlowStateImpl<T>(initialState: T) : FlowState<T>, MutableStateFlow<T> by MutableStateFlow(initialState) {

    override fun dispatch(action: FlowAction<T>) {
        this.update { currentValue ->
            action.reduce(currentValue)
        }
    }
}

fun <T> FlowState(initialState: T): FlowState<T> {
    return FlowStateImpl(initialState)
}
