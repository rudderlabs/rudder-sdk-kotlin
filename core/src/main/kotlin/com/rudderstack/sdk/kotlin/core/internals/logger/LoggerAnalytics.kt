package com.rudderstack.sdk.kotlin.core.internals.logger

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger.Companion.DEFAULT_LOG_LEVEL
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi

/**
 * `LoggerAnalytics` is a singleton class that was previously used to manage the logging instance for the SDK.
 *
 * **DEPRECATED**: This class is deprecated in favor of instance-based logging. Migrate as follows:
 *
 * ### Configuring logger and log level
 * Pass `logger` and `logLevel` via `Configuration` when initializing the SDK:
 *
 * ```kotlin
 * Analytics(
 *     configuration = Configuration(
 *         writeKey = "YOUR_WRITE_KEY",
 *         dataPlaneUrl = "YOUR_DATA_PLANE_URL",
 *         logger = AndroidLogger(),
 *         logLevel = Logger.LogLevel.VERBOSE
 *     )
 * )
 * ```
 *
 * ### Logging inside a custom plugin
 * Use the `logger` extension property available on the `Plugin` interface.
 * Inside a plugin, simply call `logger` directly:
 *
 * ```kotlin
 * class MyPlugin : Plugin {
 *     override fun intercept(event: Event): Event {
 *         logger.verbose("Processing event")
 *         return event
 *     }
 * }
 * ```
 *
 * This class remains available for backward compatibility but should not be used in new code.
 */
@Deprecated(
    message = "LoggerAnalytics is deprecated. Pass logger and logLevel via Configuration instead. " +
        "Inside a custom plugin, use the `logger` extension property available on the Plugin interface.",
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
        message = "Use instance-based logging instead. Configure logger and logLevel via Configuration, " +
            "then inside a custom plugin, call logger.verbose(log).",
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
        message = "Use instance-based logging instead. Configure logger and logLevel via Configuration, " +
            "then inside a custom plugin, call logger.debug(log).",
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
        message = "Use instance-based logging instead. Configure logger and logLevel via Configuration, " +
            "then inside a custom plugin, call logger.info(log).",
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
        message = "Use instance-based logging instead. Configure logger and logLevel via Configuration, " +
            "then inside a custom plugin, call logger.warn(log).",
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
        message = "Use instance-based logging instead. Configure logger and logLevel via Configuration, " +
            "then inside a custom plugin, call logger.error(log, throwable).",
        level = DeprecationLevel.WARNING
    )
    @JvmOverloads
    fun error(log: String, throwable: Throwable? = null) {
        if (Logger.LogLevel.ERROR >= logLevel) {
            logger?.error(log, throwable)
        }
    }
}
