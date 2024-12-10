package com.rudderstack.kotlin.core.internals.logger

/**
 * `KotlinLogger` is a concrete implementation of the `Logger` interface designed to handle logging
 * functionality for the RudderStack SDK in a Kotlin environment. This logger outputs log messages to
 * the console using `println` statements, filtered by the current log level setting.
 *
 * The logger supports five levels of logging:
 * - **VERBOSE**: Logs detailed messages for in-depth troubleshooting.
 * - **DEBUG**: Logs messages useful for debugging purposes.
 * - **INFO**: Logs informational messages that highlight the progress of the application.
 * - **WARN**: Logs potentially harmful situations that warrant attention.
 * - **ERROR**: Logs error events that might still allow the application to continue running.
 */
internal class KotlinLogger : Logger {

    override fun verbose(log: String) {
        println("$TAG-verbose : $log")
    }

    override fun info(log: String) {
        println("$TAG-info : $log")
    }

    override fun debug(log: String) {
        println("$TAG-debug : $log")
    }

    override fun warn(log: String) {
        println("$TAG-warn : $log")
    }

    override fun error(log: String, throwable: Throwable?) {
        println("$TAG-error : $log")
    }
}
