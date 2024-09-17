package com.rudderstack.core.internals.utils

import com.rudderstack.core.Analytics
import kotlinx.coroutines.launch

/**
 * Runs a suspend block on a coroutine launched in `analyticsScope` with `analyticsDispatcher`
 *
 * @param block The suspend block which needs to be executed.
 */
fun Analytics.runOnAnalyticsThread(block: suspend () -> Unit) = analyticsScope.launch(analyticsDispatcher) {
    block()
}

/**
 * Logs an error `message` and then throws a `throwable` (`IllegalStateException` by default).
 *
 * @param message The error message to be logged.
 * @param throwable The error to be thrown.
 */
fun Analytics.logAndThrowError(message: String, throwable: Throwable? = null): Nothing {
    configuration.logger.error(log = message)
    throw throwable ?: error(message)
}
