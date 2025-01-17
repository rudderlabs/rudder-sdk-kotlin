package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.Analytics
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

private const val ONLY_ONE_ELEMENT = 1

/**
 * A helper method to extract out the boilerplate code.
 * It notifies the observers when the connection is available.
 */
@InternalRudderApi
fun Analytics.notifyOnlyOnceOnConnectionAvailable(block: suspend () -> Unit) {
    this.analyticsScope.launch {
        this@notifyOnlyOnceOnConnectionAvailable.connectivityState
            .filter { it }
            .take(ONLY_ONE_ELEMENT)
            .collect { block() }
    }
}
