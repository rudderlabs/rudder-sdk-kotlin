@file:Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction", "UndocumentedPublicProperty")

package com.rudderstack.kotlin.sdk.internals.statemanagement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

interface FlowState<T> : MutableStateFlow<T> {
    suspend fun dispatch(action: FlowAction<T>)
}

private class FlowStateImpl<T>(initialState: T) : FlowState<T>, MutableStateFlow<T> by MutableStateFlow(initialState) {

    override suspend fun dispatch(action: FlowAction<T>) {
        this.update { currentValue ->
            action.reduce(currentValue)
        }
    }
}

fun <T> FlowState(state: T): FlowState<T> {
    return FlowStateImpl(state)
}
