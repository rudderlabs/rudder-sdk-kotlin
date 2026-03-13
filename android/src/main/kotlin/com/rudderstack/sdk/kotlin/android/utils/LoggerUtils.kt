package com.rudderstack.sdk.kotlin.android.utils

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger

/**
 * Logs an error `message` and then throws a `throwable` (`IllegalStateException` by default).
 *
 * @param message The error message to be logged.
 * @param throwable The error to be thrown.
 * @param logger The logger instance to use for logging the error.
 */
internal fun logAndThrowError(message: String, throwable: Throwable? = null, logger: Logger): Nothing {
    logger.error(message)
    throw throwable ?: error(message)
}
