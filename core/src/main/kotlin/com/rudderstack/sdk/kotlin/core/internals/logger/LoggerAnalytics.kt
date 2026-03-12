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
 * LoggerAnalytics.setLogger(logger = AndroidLogger())
 * // Or for Kotlin environments
 * LoggerAnalytics.setLogger(logger = KotlinLogger())
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
 *
 * **DEPRECATION NOTICE**: While this class remains available for backward compatibility with existing integrations,
 * internal SDK components now use instance-based logging via `AnalyticsLogger` to support different log levels
 * per Analytics instance. New internal development should use the instance logger available via `analyticsInstance.logger`.
 * External integrations may continue to use this singleton safely.
 */
@Deprecated(
    message = "LoggerAnalytics is deprecated for internal SDK usage and external clients. " +
        "Use analyticsInstance.logger instead " +
        "for instance-based logging. External integrations may continue using this safely for backward compatibility.",
    level = DeprecationLevel.WARNING
)
object LoggerAnalytics {

    /**
     * The logger instance used for logging messages. This is a platform-specific implementation of the Logger interface.
     */
    @InternalRudderApi
    var logger: Logger? = null
        @Synchronized private set

        @Synchronized get

    /**
     * The log level for the logger. This determines the minimum severity of messages that will be logged.
     */
    var logLevel: Logger.LogLevel = DEFAULT_LOG_LEVEL
        @Synchronized
        @Deprecated(
            message = "Pass the logLevel to the Configuration of each Analytics instance instead for instance-based logging",
            level = DeprecationLevel.WARNING
        )
        set

        @Synchronized
        get

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
    @Deprecated(
        message = "Pass the logger instance to the Configuration " +
            "of each Analytics instance instead for instance-based logging",
        level = DeprecationLevel.WARNING
    )
    fun setLogger(logger: Logger) {
        this.logger = logger
    }

    /**
     * Logs a verbose message. Verbose logs are typically used for detailed debugging information.
     *
     * @param log The message to log.
     */
    @Deprecated(
        message = "Use analyticsInstance.logger.verbose() instead for instance-based logging",
        level = DeprecationLevel.WARNING
    )
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
    @Deprecated(
        message = "Use analyticsInstance.logger.debug() instead for instance-based logging",
        level = DeprecationLevel.WARNING
    )
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
    @Deprecated(
        message = "Use analyticsInstance.logger.info() instead for instance-based logging",
        level = DeprecationLevel.WARNING
    )
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
    @Deprecated(
        message = "Use analyticsInstance.logger.warn() instead for instance-based logging",
        level = DeprecationLevel.WARNING
    )
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
    @Deprecated(
        message = "Use analyticsInstance.logger.error() instead for instance-based logging",
        level = DeprecationLevel.WARNING
    )
    @JvmOverloads
    fun error(log: String, throwable: Throwable? = null) {
        if (Logger.LogLevel.ERROR >= logLevel) {
            logger?.error(log, throwable)
        }
    }
}
