package com.rudderstack.sampleapp

import android.app.Application
import com.rudderstack.android.sampleapp.BuildConfig
import com.rudderstack.sampleapp.analytics.RudderAnalyticsUtils
import timber.log.Timber

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        initAnalyticSdk()
    }

    private fun initAnalyticSdk() {
        RudderAnalyticsUtils.initialize(this)
    }
}
