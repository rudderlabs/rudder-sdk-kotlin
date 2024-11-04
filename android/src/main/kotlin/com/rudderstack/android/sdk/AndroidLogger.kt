package com.rudderstack.android.sdk

import android.util.Log
import com.rudderstack.kotlin.sdk.internals.logger.Logger
import com.rudderstack.kotlin.sdk.internals.logger.TAG

/**
 * `AndroidLogger` is a concrete implementation of the `Logger` interface designed to handle logging
 * functionality for the RudderStack SDK in an Android environment. This logger outputs log messages
 * to the console using `Log` statements, filtered by the current log level setting.
 *
 * The logger supports five levels of logging:
 * - **VERBOSE**: Logs detailed messages that are useful for debugging.
 * - **DEBUG**: Logs detailed information that is useful for debugging.
 * - **INFO**: Logs informational messages that highlight the progress of the application.
 * - **WARN**: Logs potentially harmful situations that warrant attention.
 * - **ERROR**: Logs error events that might still allow the application to continue running.
 *
 * The logger can be activated at different log levels using the `activate` method, which controls
 * which messages are logged based on their severity.
 */
internal class AndroidLogger : Logger {

    private lateinit var logLevel: Logger.LogLevel
        @Synchronized set

        @Synchronized get

    override fun activate(level: Logger.LogLevel) {
        logLevel = level
    }

    override fun verbose(log: String) {
        if (Logger.LogLevel.VERBOSE >= logLevel) {
            Log.v(TAG, log)
        }
    }

    override fun debug(log: String) {
        if (Logger.LogLevel.DEBUG >= logLevel) {
            Log.d(TAG, log)
        }
    }

    override fun info(log: String) {
        if (Logger.LogLevel.INFO >= logLevel) {
            Log.i(TAG, log)
        }
    }

    override fun warn(log: String) {
        if (Logger.LogLevel.WARN >= logLevel) {
            Log.w(TAG, log)
        }
    }

    override fun error(log: String, throwable: Throwable?) {
        if (Logger.LogLevel.ERROR >= logLevel) {
            Log.e(TAG, log, throwable)
        }
    }

    override val level: Logger.LogLevel
        get() = logLevel
}
