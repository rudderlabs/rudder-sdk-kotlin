package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics

/**
 * Executes the given block and catches any exception that occurs.
 *
 * Utilize this method when you want to handle exceptions in a custom way by providing your own onException block.
 * Or when you need to provide some finally block.
 *
 * Use:
 * ```
 * val block = { someCode() }
 * val onException = { e: Exception -> handleException(e) }
 * val onFinally = { someCleanupCode() }
 * safelyExecute(
 *     block = block,
 *     onException = onException,
 *     onFinally = onFinally
 * )
 * ```
 *
 * **NOTE**: It catches the generic `Exception` type. Use it with caution.
 *
 * @param block The block to be executed.
 * @param onException The block to be executed when an exception occurs.
 * @param onFinally The block to be executed after the main and exception blocks are executed. Default is an empty block.
 */
@InternalRudderApi
@Suppress("TooGenericExceptionCaught")
inline fun safelyExecute(block: () -> Unit, onException: (Exception) -> Unit, onFinally: () -> Unit = {}) {
    try {
        block()
    } catch (e: Exception) {
        onException(e)
    } finally {
        onFinally()
    }
}

/**
 * Default exception handler that logs the exception along with its full stack trace.
 *
 * **NOTE**: This handler can be extended to integrate with crash reporting tools.
 *
 * @param errorMsg The error message to be logged along with the exception.
 * @param exception The exception that was thrown and needs to be handled.
 */
@InternalRudderApi
fun defaultExceptionHandler(errorMsg: String, exception: Exception) {
    LoggerAnalytics.error("$errorMsg ${exception.stackTraceToString()}")
}
