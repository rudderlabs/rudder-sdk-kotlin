package com.rudderstack.sdk.kotlin.core.internals.logger

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger.Companion.DEFAULT_LOG_LEVEL
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi

/**
 * `LoggerAnalytics` is a singleton class that manages the logging instance for the SDK, supporting configurable
 * logger types and log levels. It allows setting up either an Android or Kotlin logger, providing consistent
 * logging across different environments.
 *
 * ### Setup
 * Use the `setLogger` method to configure the logger instance:
 *
 * ```kotlin
 * LoggerAnalytics.setup(logger = AndroidLogger())
 * // Or for Kotlin environments
 * LoggerAnalytics.setup(logger = KotlinLogger())
 * ```
 * Use `logLevel` to set the desired log level:
 *
 * ```kotlin
 * LoggerAnalytics.logLevel = Logger.LogLevel.VERBOSE
 * ```
 *
 * ### Usage
 * Once configured, log messages at various levels as shown below:
 *
 * ```kotlin
 * LoggerAnalytics.verbose("This is a verbose message")
 * LoggerAnalytics.debug("This is a debug message")
 * LoggerAnalytics.info("This is an info message")
 * LoggerAnalytics.warn("This is a warning message")
 * LoggerAnalytics.error("This is an error message", throwable)
 * ```
 *
 * These methods ensure that messages are logged according to the configured log level, providing flexibility
 * and clarity for debugging and tracking events across SDK modules.
 */
object LoggerAnalytics {

    private var logger: Logger? = null

    /**
     * The log level for the logger. This determines the minimum severity of messages that will be logged.
     */
    var logLevel: Logger.LogLevel = DEFAULT_LOG_LEVEL
        @Synchronized set

        @Synchronized get

    /**
     * Sets the logger instance if null.
     */
    @InternalRudderApi
    fun setPlatformLogger(logger: Logger) {
        if (this.logger == null) {
            this.logger = logger
        }
    }

    /**
     * Sets the logger instance. This method allows you to set a custom logger for the SDK.
     *
     * @param logger The logger instance to set.
     */
    fun setLogger(logger: Logger) {
        this.logger = logger
    }

    /**
     * Logs a verbose message. Verbose logs are typically used for detailed debugging information.
     *
     * @param log The message to log.
     */
    fun verbose(log: String) {
        if (Logger.LogLevel.VERBOSE >= logLevel) {
            logger?.verbose(log)
        }
    }

    /**
     * Logs a debug message, used primarily for debugging purposes.
     *
     * @param log The message to log.
     */
    fun debug(log: String) {
        if (Logger.LogLevel.DEBUG >= logLevel) {
            logger?.debug(log)
        }
    }

    /**
     * Logs an informational message, generally used to indicate normal operations.
     *
     * @param log The message to log.
     */
    fun info(log: String) {
        if (Logger.LogLevel.INFO >= logLevel) {
            logger?.info(log)
        }
    }

    /**
     * Logs a warning message, typically used to highlight potential issues.
     *
     * @param log The message to log.
     */
    fun warn(log: String) {
        if (Logger.LogLevel.WARN >= logLevel) {
            logger?.warn(log)
        }
    }

    /**
     * Logs an error message, generally used for significant issues that need attention.
     *
     * @param log The message to log.
     * @param throwable An optional exception to log alongside the error message.
     */
    fun error(log: String, throwable: Throwable? = null) {
        if (Logger.LogLevel.ERROR >= logLevel) {
            logger?.error(log, throwable)
        }
    }
}
