@file:Suppress("UndocumentedPublicFunction", "UndocumentedPublicClass")

package com.rudderstack.kotlin.sdk.internals.statemanagement

fun interface FlowAction<T> {
    fun reduce(currentState: T): T
}
