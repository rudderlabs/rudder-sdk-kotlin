package com.rudderstack.integration.kotlin.facebook

import com.facebook.appevents.AppEventsLogger
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.utils.application
import kotlinx.serialization.json.JsonObject

internal const val FACEBOOK_KEY = "Facebook App Events"

class FacebookIntegration : IntegrationPlugin() {

    private var facebookAppEventsLogger: AppEventsLogger? = null

    override val key: String
        get() = FACEBOOK_KEY

    override fun create(destinationConfig: JsonObject) {
        if (facebookAppEventsLogger == null) {
            facebookAppEventsLogger = AppEventsLogger.newLogger(analytics.application)
        }
    }

    override fun getDestinationInstance(): Any? {
        return facebookAppEventsLogger
    }
}
