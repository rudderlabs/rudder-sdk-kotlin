package com.rudderstack.sampleapp.analytics

import android.app.Application
import com.rudderstack.android.sdk.Analytics
import com.rudderstack.android.sdk.Configuration
import com.rudderstack.kotlin.sdk.internals.logger.Logger

object RudderAnalyticsUtils {

    lateinit var analytics: Analytics

    fun initialize(application: Application) {
        analytics = Analytics(
            configuration = Configuration(
                trackApplicationLifecycleEvents = true,
                writeKey = "<WRITE_KEY>",
                application = application,
                dataPlaneUrl = "<DATA_PLANE_URL>",
                logLevel = Logger.LogLevel.VERBOSE,
            )
        )
    }
}
