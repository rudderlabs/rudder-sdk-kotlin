package com.rudderstack.android.kotlin_jvm_app

import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.Configuration
import com.rudderstack.kotlin.sdk.Configuration.Companion.DEFAULT_GZIP_STATUS
import com.rudderstack.kotlin.sdk.internals.logger.KotlinLogger
import com.rudderstack.kotlin.sdk.internals.logger.Logger
import com.rudderstack.kotlin.sdk.internals.logger.TAG
import com.rudderstack.kotlin.sdk.internals.models.Properties
import com.rudderstack.kotlin.sdk.internals.models.RudderOption
import com.rudderstack.kotlin.sdk.internals.models.RudderTraits
import java.util.Date

private lateinit var analytics: Analytics

fun main() {
    analytics = Analytics(
        configuration = Configuration(
            writeKey = "<WRITE KEY>",
            dataPlaneUrl = "<DATA PLANE URL>",
            logger = KotlinLogger(initialLogLevel = Logger.LogLevel.DEBUG),
            optOut = false,
            gzipEnabled = DEFAULT_GZIP_STATUS,
        )
    )

    trackMessageKotlinAPI(analytics)
}

fun trackMessageKotlinAPI(analytics: Analytics) {
    analytics.track(
        name = "Track at ${Date()}",
        properties = Properties(emptyMap()),
        options = RudderOption(),
    )

    analytics.screen(
        screenName = "Screen at ${Date()}",
        category = "Main",
        properties = Properties(emptyMap()),
        options = RudderOption(),
    )

    analytics.group(
        groupId = "Group at ${Date()}",
        traits = RudderTraits(emptyMap()),
        options = RudderOption()
    )
    analytics.configuration.logger.debug(tag = TAG, log = "Message sent")
}
