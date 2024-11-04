package com.rudderstack.kotlin.sdk.internals.logger

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
internal class KotlinLogger : Logger {

    private lateinit var logLevel: Logger.LogLevel
        @Synchronized set

        @Synchronized get

    override fun activate(level: Logger.LogLevel) {
        logLevel = level
    }

    override fun verbose(log: String) {
        if (Logger.LogLevel.VERBOSE >= logLevel) {
            println("$TAG-verbose : $log")
        }
    }

    override fun info(log: String) {
        if (Logger.LogLevel.INFO >= logLevel) {
            println("$TAG-info : $log")
        }
    }

    override fun debug(log: String) {
        if (Logger.LogLevel.DEBUG >= logLevel) {
            println("$TAG-debug : $log")
        }
    }

    override fun warn(log: String) {
        if (Logger.LogLevel.WARN >= logLevel) {
            println("$TAG-warn : $log")
        }
    }

    override fun error(log: String, throwable: Throwable?) {
        if (Logger.LogLevel.ERROR >= logLevel) {
            println("$TAG-error : $log")
        }
    }

    override val level: Logger.LogLevel
        get() = logLevel
}
