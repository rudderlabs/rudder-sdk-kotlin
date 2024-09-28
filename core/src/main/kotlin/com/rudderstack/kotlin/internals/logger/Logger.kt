package com.rudderstack.kotlin.internals.logger

/**
 * TAG is the default tag used for logging in the RudderStack SDK.
 * */
const val TAG = "Rudder-Analytics"

/**
 * `Logger` is an interface that defines a standard logging mechanism for the RudderStack SDK.
 * It provides methods to log messages at different levels (INFO, DEBUG, WARN, ERROR) and to
 * control the logging behavior by setting the log level.
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
     * Activates the logger with a specific log level. Only messages at this level or higher
     * (in terms of severity) will be logged.
     *
     * @param level The log level to activate for logging. It could be `DEBUG`, `INFO`, `WARN`, `ERROR`, or `NONE`.
     */
    fun activate(level: LogLevel)

    /**
     * Logs an informational message. This level is typically used to highlight the progress of the application.
     *
     * @param tag A tag to identify the source of a log message. Default is "Rudder-Analytics".
     * @param log The message to be logged.
     */
    fun info(tag: String = TAG, log: String)

    /**
     * Logs a debug message. This level is typically used for detailed debugging information.
     *
     * @param tag A tag to identify the source of a log message. Default is "Rudder-Analytics".
     * @param log The message to be logged.
     */
    fun debug(tag: String = TAG, log: String)

    /**
     * Logs a warning message. This level is typically used to log potentially harmful situations.
     *
     * @param tag A tag to identify the source of a log message. Default is "Rudder-Analytics".
     * @param log The message to be logged.
     */
    fun warn(tag: String = TAG, log: String)

    /**
     * Logs an error message. This level is typically used to log error events that might still allow
     * the application to continue running.
     *
     * @param tag A tag to identify the source of a log message. Default is "Rudder-Analytics".
     * @param log The message to be logged.
     * @param throwable An optional throwable associated with the error being logged.
     */
    fun error(tag: String = TAG, log: String, throwable: Throwable? = null)

    /**
     * Enum representing the different log levels that can be set for the logger.
     * - `DEBUG`: Log detailed information useful for debugging.
     * - `INFO`: Log general information about the application's progress.
     * - `WARN`: Log potentially harmful situations.
     * - `ERROR`: Log error events.
     * - `NONE`: Disable logging.
     */
    enum class LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        NONE,
    }

    /**
     * A property to get the current log level of the logger.
     */
    val level: LogLevel
}
