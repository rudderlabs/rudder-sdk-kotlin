package com.rudderstack.sampleapp.analytics.migration

import android.util.Log

/**
 * Centralized logging utility for the RudderStack SDK migration system.
 *
 * This logger provides simple, flexible logging methods that can be used
 * throughout the migration process and other parts of the application.
 *
 * ## Usage
 * ```kotlin
 * // Set log level (default is INFO)
 * MigrationLogger.logLevel = LogLevel.DEBUG
 *
 * // Log messages at different levels
 * MigrationLogger.verbose("Detailed trace information")
 * MigrationLogger.debug("Debug information")
 * MigrationLogger.info("General informational message")
 * MigrationLogger.warn("Warning message")
 * MigrationLogger.error("Error message")
 * ```
 *
 * ## Log Levels
 * - `VERBOSE`: Very detailed trace information
 * - `DEBUG`: Detailed information for debugging
 * - `INFO`: General informational messages (default)
 * - `WARN`: Warning messages for non-critical issues
 * - `ERROR`: Error messages for critical failures
 * - `NONE`: Disable all logging
 *
 * @see LogLevel
 */
object MigrationLogger {
    private const val TAG = "RudderMigration"

    /**
     * Current logging level. Messages below this level will be suppressed.
     * Default is [LogLevel.INFO].
     */
    var logLevel: LogLevel = LogLevel.VERBOSE

    /**
     * Log level enumeration with priority ordering.
     *
     * @property priority Numeric priority for level comparison (lower = more verbose)
     */
    enum class LogLevel(val priority: Int) {
        /** Very detailed trace information */
        VERBOSE(0),

        /** Detailed debug information */
        DEBUG(1),

        /** General informational messages */
        INFO(2),

        /** Warning messages */
        WARN(3),

        /** Error messages */
        ERROR(4),

        /** Suppress all logging */
        NONE(5)
    }

    /**
     * Log a verbose message if current log level allows.
     * Use for very detailed trace information.
     *
     * @param message The message to log
     */
    fun verbose(message: String) {
        if (logLevel.priority <= LogLevel.VERBOSE.priority) {
            Log.v(TAG, message)
        }
    }

    /**
     * Log a debug message if current log level allows.
     * Use for detailed information helpful during development and debugging.
     *
     * @param message The message to log
     */
    fun debug(message: String) {
        if (logLevel.priority <= LogLevel.DEBUG.priority) {
            Log.d(TAG, message)
        }
    }

    /**
     * Log an info message if current log level allows.
     * Use for general informational messages about normal operations.
     *
     * @param message The message to log
     */
    fun info(message: String) {
        if (logLevel.priority <= LogLevel.INFO.priority) {
            Log.i(TAG, message)
        }
    }

    /**
     * Log a warning message if current log level allows.
     * Use for potentially harmful situations or non-critical issues.
     *
     * @param message The message to log
     */
    fun warn(message: String) {
        if (logLevel.priority <= LogLevel.WARN.priority) {
            Log.w(TAG, message)
        }
    }

    /**
     * Log an error message if current log level allows.
     * Use for error events that might still allow the application to continue.
     *
     * @param message The message to log
     */
    fun error(message: String) {
        if (logLevel.priority <= LogLevel.ERROR.priority) {
            Log.e(TAG, message)
        }
    }
}
