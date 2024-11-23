package com.rudderstack.sampleapp.analytics

import android.app.Application
import com.rudderstack.android.sdk.Analytics
import com.rudderstack.android.sdk.Configuration
import com.rudderstack.android.sdk.SessionConfiguration
import com.rudderstack.kotlin.sdk.internals.logger.Logger
import com.rudderstack.kotlin.sdk.internals.models.RudderOption
import com.rudderstack.sampleapp.analytics.customplugins.AndroidAdvertisingIdPlugin
import com.rudderstack.sampleapp.analytics.customplugins.AndroidAdvertisingIdPlugin.Companion.isAdvertisingLibraryAvailable
import com.rudderstack.sampleapp.analytics.customplugins.OptionPlugin
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object RudderAnalyticsUtils {

    lateinit var analytics: Analytics

    fun initialize(application: Application) {
        analytics = Analytics(
            configuration = Configuration(
                trackApplicationLifecycleEvents = true,
                writeKey = "2lEGUmhhp9rKcvW21NuNsZ6PJwo",
                application = application,
                dataPlaneUrl = "https://rudderstachdvs.dataplane.rudderstack.com",
                sessionConfiguration = SessionConfiguration(
                    automaticSessionTracking = true,
                    sessionTimeoutInMillis = 3000,
                ),
                logLevel = Logger.LogLevel.VERBOSE,
            )
        )
        if (isAdvertisingLibraryAvailable()) {
            analytics.add(AndroidAdvertisingIdPlugin())
        }
        analytics.add(OptionPlugin(
            option = RudderOption(
                customContext = buildJsonObject {
                    put("key", "value")
                },
                integrations = mapOf(
                    "CleverTap" to true
                ),
            )
        ))
    }
}
