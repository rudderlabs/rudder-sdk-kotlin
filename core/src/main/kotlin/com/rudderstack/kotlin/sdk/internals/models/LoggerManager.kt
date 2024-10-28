package com.rudderstack.kotlin.sdk.internals.models

import com.rudderstack.kotlin.sdk.internals.logger.Logger

/**
 * `LoggerManager` is a singleton class that manages the logging instance for the SDK, supporting configurable
 * logger types and log levels. It allows setting up either an Android or Kotlin logger, providing consistent
 * logging across different environments.
 *
 * ### Setup
 * Use the `setLogger` method to configure the logger instance and specify a log level:
 *
 * ```kotlin
 * LoggerManager.setLogger(logger = AndroidLogger, level = configuration.logLevel)
 * // Or for non-Android environments
 * LoggerManager.setLogger(logger = KotlinLogger, level = configuration.logLevel)
 * ```
 *
 * Optionally, set a custom tag for the logger using `setTag`:
 *
 * ```kotlin
 * LoggerManager.setTag("RudderStack")
 * ```
 *
 * ### Usage
 * Once configured, log messages at various levels as shown below:
 *
 * ```kotlin
 * LoggerManager.verbose("This is a verbose message")
 * LoggerManager.debug("This is a debug message")
 * LoggerManager.info("This is an info message")
 * LoggerManager.warn("This is a warning message")
 * LoggerManager.error("This is an error message", throwable)
 * ```
 *
 * These methods ensure that messages are logged according to the configured log level, providing flexibility
 * and clarity for debugging and tracking events across SDK modules.
 */
object LoggerManager {
    private lateinit var logger: Logger

    /**
     * Sets the logger instance and log level for the SDK.
     *
     * @param logger The logger instance to use (e.g., `AndroidLogger` or `KotlinLogger`).
     * @param level The log level to activate for the logger, defining the minimum severity of logs to display.
     */
    fun setLogger(logger: Logger, level: Logger.LogLevel) {
        this.logger = logger
        logger.activate(level)
    }

    /**
     * Sets a custom tag for the logger instance, which can be used to group or identify logs.
     *
     * @param tag A string tag to associate with all log messages.
     */
    fun setTag(tag: String) {
        logger.setTag(tag)
    }

    /**
     * Logs a verbose message. Verbose logs are typically used for detailed debugging information.
     *
     * @param log The message to log.
     */
    fun verbose(log: String) {
        logger.verbose(log)
    }

    /**
     * Logs a debug message, used primarily for debugging purposes.
     *
     * @param log The message to log.
     */
    fun debug(log: String) {
        logger.debug(log)
    }

    /**
     * Logs an informational message, generally used to indicate normal operations.
     *
     * @param log The message to log.
     */
    fun info(log: String) {
        logger.info(log)
    }

    /**
     * Logs a warning message, typically used to highlight potential issues.
     *
     * @param log The message to log.
     */
    fun warn(log: String) {
        logger.warn(log)
    }

    /**
     * Logs an error message, generally used for significant issues that need attention.
     *
     * @param log The message to log.
     * @param throwable An optional exception to log alongside the error message.
     */
    fun error(log: String, throwable: Throwable? = null) {
        logger.error(log, throwable)
    }
}
