@file:Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction", "UndocumentedPublicProperty")

package com.rudderstack.kotlin.sdk.internals.statemanagement

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

interface FlowState<T> : MutableStateFlow<T> {

    val scope: CoroutineScope
    val dispatcher: CoroutineDispatcher
    fun dispatch(action: FlowAction<T>)
}

private class FlowStateImpl<T>(initialState: T) : FlowState<T>, MutableStateFlow<T> by MutableStateFlow(initialState) {

    override lateinit var scope: CoroutineScope
    override lateinit var dispatcher: CoroutineDispatcher
    override fun dispatch(action: FlowAction<T>) {
        this.update { currentValue ->
            action.reduce(currentValue, scope, dispatcher)
        }
    }
}

fun <T> FlowState(initialState: T, scope: CoroutineScope, dispatcher: CoroutineDispatcher): FlowState<T> {
    return FlowStateImpl(initialState).also {
        it.scope = scope
        it.dispatcher = dispatcher
    }
}
