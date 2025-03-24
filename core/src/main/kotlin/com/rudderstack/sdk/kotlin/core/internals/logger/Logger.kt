package com.rudderstack.sdk.kotlin.core.internals.logger

/**
 * `Logger` is an interface that defines a standard logging mechanism for the RudderStack SDK.
 * It provides methods to log messages at different levels (VERBOSE, DEBUG, INFO, WARN and ERROR).
 */
interface Logger {

    companion object {
        /**
         * The default log level for the logger, set to `LogLevel.NONE`. This means that by default,
         * no logs will be output.
         */
        @JvmField
        val DEFAULT_LOG_LEVEL = LogLevel.NONE
    }

    /**
     * Logs a verbose message. This level is typically used for detailed information.
     *
     * @param log The message to be logged.
     */
    fun verbose(log: String)

    /**
     * Logs a debug message. This level is typically used for debugging purposes.
     *
     * @param log The message to be logged.
     */
    fun debug(log: String)

    /**
     * Logs an informational message. This level is typically used to highlight the progress of the application.
     *
     * @param log The message to be logged.
     */
    fun info(log: String)

    /**
     * Logs a warning message. This level is typically used to log potentially harmful situations.
     *
     * @param log The message to be logged.
     */
    fun warn(log: String)

    /**
     * Logs an error message. This level is typically used to log error events that might still allow
     * the application to continue running.
     *
     * @param log The message to be logged.
     * @param throwable An optional throwable associated with the error being logged.
     */
    fun error(log: String, throwable: Throwable? = null)

    /**
     * Enum representing the different log levels that can be set for the logger.
     * - `DEBUG`: Log detailed information useful for debugging.
     * - `INFO`: Log general information about the application's progress.
     * - `WARN`: Log potentially harmful situations.
     * - `ERROR`: Log error events.
     * - `NONE`: Disable logging.
     */
    enum class LogLevel {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        NONE,
    }
}
