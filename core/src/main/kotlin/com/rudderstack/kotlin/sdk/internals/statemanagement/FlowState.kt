@file:Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")

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

fun <T> FlowState(initialState: T): FlowState<T> {
    return FlowStateImpl(initialState)
}
