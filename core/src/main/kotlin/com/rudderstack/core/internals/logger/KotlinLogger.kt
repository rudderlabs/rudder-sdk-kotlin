package com.rudderstack.core.internals.logger

/**
 * `KotlinLogger` is a concrete implementation of the `Logger` interface designed to handle logging
 * functionality for the RudderStack SDK in a Kotlin environment. This logger outputs log messages to
 * the console using `println` statements, filtered by the current log level setting.
 *
 * The logger supports four levels of logging:
 * - **INFO**: Logs informational messages that highlight the progress of the application.
 * - **DEBUG**: Logs detailed information that is useful for debugging.
 * - **WARN**: Logs potentially harmful situations that warrant attention.
 * - **ERROR**: Logs error events that might still allow the application to continue running.
 *
 * The logger can be activated at different log levels using the `activate` method, which controls
 * which messages are logged based on their severity.
 *
 * @property initialLogLevel The initial log level that the logger starts with. The default is `Logger.DEFAULT_LOG_LEVEL`.
 */
class KotlinLogger(initialLogLevel: Logger.LogLevel = Logger.DEFAULT_LOG_LEVEL) : Logger {

    private var logLevel = initialLogLevel

    override fun activate(level: Logger.LogLevel) {
        logLevel = level
    }

    override fun info(tag: String, log: String) {
        if (com.rudderstack.core.internals.logger.Logger.LogLevel.INFO >= logLevel) {
            println("$tag-info : $log")
        }
    }

    override fun debug(tag: String, log: String) {
        if (com.rudderstack.core.internals.logger.Logger.LogLevel.DEBUG >= logLevel) {
            println("$tag-debug : $log")
        }
    }

    override fun warn(tag: String, log: String) {
        if (com.rudderstack.core.internals.logger.Logger.LogLevel.WARN >= logLevel) {
            println("$tag-warn : $log")
        }
    }

    override fun error(tag: String, log: String, throwable: Throwable?) {
        if (com.rudderstack.core.internals.logger.Logger.LogLevel.ERROR >= logLevel) {
            println("$tag-error : $log")
        }
    }

    override val level: Logger.LogLevel
        get() = logLevel
}
