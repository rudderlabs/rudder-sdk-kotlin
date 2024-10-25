@file:Suppress("UndocumentedPublicFunction", "UndocumentedPublicClass")

package com.rudderstack.kotlin.sdk.internals.statemanagement

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

fun interface FlowAction<T> {
    fun reduce(currentState: T, scope: CoroutineScope, dispatcher: CoroutineDispatcher): T
}
