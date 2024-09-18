package com.rudderstack.sampleapp.analytics

import android.app.Application
import com.rudderstack.android.Analytics

object RudderAnalyticsUtils {

    lateinit var analytics: Analytics

    fun initialize(application: Application) {
        analytics = Analytics(
            configuration = com.rudderstack.android.Configuration(
                writeKey = "2bdKKpiBaIwBuqdAyH6OpUc1bXD",
                application = application,
                dataPlaneUrl = "https://rudderstaciwbf.dataplane.rudderstack.com",
            )
        )
    }
}