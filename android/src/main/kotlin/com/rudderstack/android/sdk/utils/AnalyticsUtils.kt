package com.rudderstack.android.sdk.utils

import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.logger.LoggerAnalytics
import kotlinx.coroutines.launch

/**
 * Runs a suspend block on a coroutine launched in `analyticsScope` with `analyticsDispatcher`
 *
 * @param block The suspend block which needs to be executed.
 */
internal fun Analytics.runOnAnalyticsThread(block: suspend () -> Unit) = analyticsScope.launch(analyticsDispatcher) {
    block()
}

/**
 * Logs an error `message` and then throws a `throwable` (`IllegalStateException` by default).
 *
 * @param message The error message to be logged.
 * @param throwable The error to be thrown.
 */
internal fun Analytics.logAndThrowError(message: String, throwable: Throwable? = null): Nothing {
    LoggerAnalytics.error(message)
    throw throwable ?: error(message)
}
