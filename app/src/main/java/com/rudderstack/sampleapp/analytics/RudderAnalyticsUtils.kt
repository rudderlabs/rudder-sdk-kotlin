package com.rudderstack.sampleapp.analytics

import android.app.Application
import com.rudderstack.android.sampleapp.BuildConfig
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.SessionConfiguration
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
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
                writeKey = BuildConfig.WRITE_KEY,
                application = application,
                dataPlaneUrl = BuildConfig.DATA_PLANE_URL,
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
                integrations = buildJsonObject {
                    put("CleverTap", true)
                }
            )
        ))
    }
}
