package com.rudderstack.sdk.kotlin.core.internals.logger

import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi

/**
 * `AnalyticsLogger` is an instance-based logger that allows each Analytics instance to have its own logging configuration.
 * This enables multiple Analytics instances to have different log levels (e.g., one in DEBUG mode and another in NONE).
 *
 * This class holds a logLevel and a reference to the platform-specific logger interface (Logger) and implements
 * methods that check the instance's logLevel before delegating to the platform logger.
 *
 * ### Setup
 * The logger is created internally by the `Analytics` class using the `logger` and `logLevel` from the `Configuration`.
 * Configure these values when building the `Configuration`:
 *
 * ```kotlin
 * val configuration = Configuration(
 *     // ...
 *     logger = AndroidLogger(),  // or KotlinLogger()
 *     logLevel = Logger.LogLevel.VERBOSE
 * )
 * val analytics = Analytics(configuration)
 * ```
 *
 * ### Usage
 * Access the logger via `Analytics.logger` or the `Plugin.logger` extension, then log
 * messages at various levels:
 *
 * ```kotlin
 * analytics.logger.verbose("This is a verbose message")
 * analytics.logger.debug("This is a debug message")
 * analytics.logger.info("This is an info message")
 * analytics.logger.warn("This is a warning message")
 * analytics.logger.error("This is an error message", throwable)
 * ```
 *
 * The methods ensure that messages are logged according to the configured log level, providing flexibility
 * and clarity for debugging and tracking events across SDK modules on a per-instance basis.
 */
internal class AnalyticsLogger(
    private val logger: Logger,
    private val logLevel: Logger.LogLevel
) : Logger {

    override fun verbose(log: String) {
        if (Logger.LogLevel.VERBOSE >= logLevel) {
            logger.verbose(log)
        }
    }

    override fun debug(log: String) {
        if (Logger.LogLevel.DEBUG >= logLevel) {
            logger.debug(log)
        }
    }

    override fun info(log: String) {
        if (Logger.LogLevel.INFO >= logLevel) {
            logger.info(log)
        }
    }

    override fun warn(log: String) {
        if (Logger.LogLevel.WARN >= logLevel) {
            logger.warn(log)
        }
    }

    override fun error(log: String, throwable: Throwable?) {
        if (Logger.LogLevel.ERROR >= logLevel) {
            logger.error(log, throwable)
        }
    }
}

/**
 * Creates an [AnalyticsLogger] that wraps the given [logger] with log-level filtering.
 */
@InternalRudderApi
fun provideAnalyticsLogger(logger: Logger, logLevel: Logger.LogLevel): Logger {
    return AnalyticsLogger(logger = logger, logLevel = logLevel)
}
