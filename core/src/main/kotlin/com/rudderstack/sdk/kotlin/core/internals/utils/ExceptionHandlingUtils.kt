package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics

/**
 * Executes the given block and catches any exception that occurs.
 *
 * **NOTE**: It catches the generic `Exception` type. Use it with caution.
 *
 * @param block The block to be executed.
 * @param onException The block to be executed when an exception occurs. Default is to log the exception.
 */
@InternalRudderApi
@Suppress("TooGenericExceptionCaught")
fun safelyExecute(
    block: () -> Unit,
    onException: (Exception) -> Unit = { LoggerAnalytics.error("Exception occurred: $it") }
) {
    try {
        block()
    } catch (e: Exception) {
        onException(e)
    }
}
