package com.rudderstack.sampleapp.analytics

import android.util.Log
import com.rudderstack.kotlin.sdk.internals.logger.Logger

var CUSTOM_TAG = "Custom-Analytics"
    private set

internal class CustomLogger : Logger {

    override fun verbose(log: String) {
        Log.v(CUSTOM_TAG, log)
    }

    override fun debug(log: String) {
        Log.d(CUSTOM_TAG, log)
    }

    override fun info(log: String) {
        Log.i(CUSTOM_TAG, log)
    }

    override fun warn(log: String) {
        Log.w(CUSTOM_TAG, log)
    }

    override fun error(log: String, throwable: Throwable?) {
        Log.e(CUSTOM_TAG, log, throwable)
    }
}
