package com.rudderstack.android.sdk

import android.util.Log
import com.rudderstack.kotlin.sdk.internals.logger.Logger
import com.rudderstack.kotlin.sdk.internals.logger.TAG

/**
 * Logger implementation specifically for android.
 */
internal object AndroidLogger : Logger {

    private var logLevel: Logger.LogLevel = Logger.DEFAULT_LOG_LEVEL
        @Synchronized set

        @Synchronized get

    override fun activate(level: Logger.LogLevel) {
        logLevel = level
    }

    override fun verbose(log: String) {
        if (Logger.LogLevel.VERBOSE >= logLevel) {
            Log.v(TAG, log)
        }
    }

    override fun debug(log: String) {
        if (Logger.LogLevel.DEBUG >= logLevel) {
            Log.d(TAG, log)
        }
    }

    override fun info(log: String) {
        if (Logger.LogLevel.INFO >= logLevel) {
            Log.i(TAG, log)
        }
    }

    override fun warn(log: String) {
        if (Logger.LogLevel.WARN >= logLevel) {
            Log.w(TAG, log)
        }
    }

    override fun error(log: String, throwable: Throwable?) {
        if (Logger.LogLevel.ERROR >= logLevel) {
            Log.e(TAG, log, throwable)
        }
    }

    override val level: Logger.LogLevel
        get() = logLevel
}
