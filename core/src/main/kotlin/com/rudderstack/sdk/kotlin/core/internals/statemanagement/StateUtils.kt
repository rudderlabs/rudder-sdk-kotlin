package com.rudderstack.sdk.kotlin.core.internals.statemanagement

import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop

private const val ONE_COUNT = 1

/**
 * Drops the initial state/value of the [State] and emits the subsequent states. This is useful when you don't want to use
 * the default initial value of a [State] variable.
 *
 * @return [Flow] of the [State] after dropping the initial value.
 */
@InternalRudderApi
fun <T> Flow<T>.dropInitialState(): Flow<T> = this.drop(ONE_COUNT)
