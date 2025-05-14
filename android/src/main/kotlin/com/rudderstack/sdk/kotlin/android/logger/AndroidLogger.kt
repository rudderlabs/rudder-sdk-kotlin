package com.rudderstack.sdk.kotlin.android.logger

import android.util.Log
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger

/**
 * `AndroidLogger` is a concrete implementation of the `Logger` interface designed to handle logging
 * functionality for the RudderStack SDK in an Android environment. This logger outputs log messages
 * to the console using `Log` statements, filtered by the current log level setting.
 *
 * The logger supports five levels of logging:
 * - **VERBOSE**: Logs detailed messages for in-depth troubleshooting.
 * - **DEBUG**: Logs messages useful for debugging purposes.
 * - **INFO**: Logs informational messages that highlight the progress of the application.
 * - **WARN**: Logs potentially harmful situations that warrant attention.
 * - **ERROR**: Logs error events that might still allow the application to continue running.
 */
internal class AndroidLogger : Logger {

    private val tag = "Rudder-Analytics"

    override fun verbose(log: String) {
        Log.v(tag, log)
    }

    override fun debug(log: String) {
        Log.d(tag, log)
    }

    override fun info(log: String) {
        Log.i(tag, log)
    }

    override fun warn(log: String) {
        Log.w(tag, log)
    }

    override fun error(log: String, throwable: Throwable?) {
        Log.e(tag, log, throwable)
    }
}
