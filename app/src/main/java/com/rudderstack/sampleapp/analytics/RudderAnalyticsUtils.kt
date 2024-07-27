package com.rudderstack.sampleapp.analytics

import android.app.Application
import com.rudderstack.android.Analytics

object RudderAnalyticsUtils {

    lateinit var analytics: Analytics

    fun initialize(application: Application) {
        analytics = Analytics(
            application = application,
            configuration = com.rudderstack.android.Configuration(
                writeKey = "sdfsdfsd",
                application = application,
                dataPlaneUrl = "<DATA_PLANE_URL>",
            )
        )
    }

    fun interface InitializationListener {

        fun onAnalyticsInitialized(writeKey: String, success: Boolean, message: String?)
    }

}
