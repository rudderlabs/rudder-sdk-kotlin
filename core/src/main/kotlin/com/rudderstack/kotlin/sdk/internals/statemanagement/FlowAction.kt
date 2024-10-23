@file:Suppress("UndocumentedPublicFunction", "UndocumentedPublicClass")

package com.rudderstack.kotlin.sdk.internals.statemanagement

fun interface FlowAction<T> {
    suspend fun reduce(currentState: T): T
}
