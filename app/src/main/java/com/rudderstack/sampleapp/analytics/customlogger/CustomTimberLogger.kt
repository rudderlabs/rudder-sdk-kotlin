package com.rudderstack.sampleapp.analytics.customlogger

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import timber.log.Timber

/**
 * Custom logger implementation using Timber for logging.
 */
internal class CustomTimberLogger : Logger {

    private val tag = "Rudder-Analytics"

    override fun verbose(log: String) {
        Timber.tag(tag).v(log)
    }

    override fun debug(log: String) {
        Timber.tag(tag).d(log)
    }

    override fun info(log: String) {
        Timber.tag(tag).i(log)
    }

    override fun warn(log: String) {
        Timber.tag(tag).w(log)
    }

    override fun error(log: String, throwable: Throwable?) {
        Timber.tag(tag).e(throwable, log)
    }
}
