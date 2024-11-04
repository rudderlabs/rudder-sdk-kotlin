package com.rudderstack.sampleapp.analytics

import android.util.Log
import com.rudderstack.kotlin.sdk.internals.logger.Logger

var CHRYSSA_TAG = "Chryssa-Analytics"
    private set

internal object ChryssaLogger : Logger {

    private var logLevel: Logger.LogLevel = Logger.DEFAULT_LOG_LEVEL
        @Synchronized set
        @Synchronized get

    override fun activate(level: Logger.LogLevel) {
        logLevel = level
    }

    override fun verbose(log: String) {
        if (Logger.LogLevel.VERBOSE >= logLevel) {
            Log.v(CHRYSSA_TAG, log)
        }
    }

    override fun debug(log: String) {
        if (Logger.LogLevel.DEBUG >= logLevel) {
            Log.d(CHRYSSA_TAG, log)
        }
    }

    override fun info(log: String) {
        if (Logger.LogLevel.INFO >= logLevel) {
            Log.i(CHRYSSA_TAG, log)
        }
    }

    override fun warn(log: String) {
        if (Logger.LogLevel.WARN >= logLevel) {
            Log.w(CHRYSSA_TAG, log)
        }
    }

    override fun error(log: String, throwable: Throwable?) {
        if (Logger.LogLevel.ERROR >= logLevel) {
            Log.e(CHRYSSA_TAG, log, throwable)
        }
    }

    override val level: Logger.LogLevel
        get() = logLevel
}
