package com.rudderstack.android.sdk.utils

import com.rudderstack.kotlin.sdk.internals.logger.LoggerAnalytics

/**
 * Logs an error `message` and then throws a `throwable` (`IllegalStateException` by default).
 *
 * @param message The error message to be logged.
 * @param throwable The error to be thrown.
 */
internal fun logAndThrowError(message: String, throwable: Throwable? = null): Nothing {
    LoggerAnalytics.error(message)
    throw throwable ?: error(message)
}
