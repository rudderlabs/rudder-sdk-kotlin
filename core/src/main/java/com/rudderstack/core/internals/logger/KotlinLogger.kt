package com.rudderstack.core.internals.logger

class KotlinLogger(initialLogLevel: Logger.LogLevel = Logger.DEFAULT_LOG_LEVEL) : Logger {

    private var logLevel = initialLogLevel

    override fun activate(level: Logger.LogLevel) {
        logLevel = level
    }

    override fun info(tag: String, log: String) {
        if (com.rudderstack.core.internals.logger.Logger.LogLevel.INFO >= logLevel)
            println("$tag-info : $log")
    }

    override fun debug(tag: String, log: String) {
        if (com.rudderstack.core.internals.logger.Logger.LogLevel.DEBUG >= logLevel)
            println("$tag-debug : $log")
    }

    override fun warn(tag: String, log: String) {
        if (com.rudderstack.core.internals.logger.Logger.LogLevel.WARN >= logLevel)
            println("$tag-warn : $log")
    }

    override fun error(tag: String, log: String, throwable: Throwable?) {
        if (com.rudderstack.core.internals.logger.Logger.LogLevel.ERROR >= logLevel)
            println("$tag-error : $log")
    }

    override val level: Logger.LogLevel
        get() = logLevel
}
