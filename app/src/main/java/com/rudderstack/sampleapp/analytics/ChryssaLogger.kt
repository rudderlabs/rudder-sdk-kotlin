package com.rudderstack.sampleapp.analytics

import android.util.Log
import com.rudderstack.kotlin.sdk.internals.logger.Logger

var CHRYSSA_TAG = "Chryssa-Analytics"
    private set

internal class ChryssaLogger : Logger {

    override fun verbose(log: String) {
        Log.v(CHRYSSA_TAG, log)
    }

    override fun debug(log: String) {
        Log.d(CHRYSSA_TAG, log)
    }

    override fun info(log: String) {
        Log.i(CHRYSSA_TAG, log)
    }

    override fun warn(log: String) {
        Log.w(CHRYSSA_TAG, log)
    }

    override fun error(log: String, throwable: Throwable?) {
        Log.e(CHRYSSA_TAG, log, throwable)
    }
}
