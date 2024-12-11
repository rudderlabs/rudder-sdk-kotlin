package com.rudderstack.android.kotlin_jvm_app

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.Configuration
import com.rudderstack.sdk.kotlin.core.Configuration.Companion.DEFAULT_GZIP_STATUS
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Properties
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.RudderTraits
import java.util.Date

private lateinit var analytics: Analytics

fun main() {
    analytics = Analytics(
        configuration = Configuration(
            writeKey = "<WRITE KEY>",
            dataPlaneUrl = "<DATA PLANE URL>",
            logLevel = Logger.LogLevel.VERBOSE,
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

    analytics.identify(
        userId = "User 1",
        traits = RudderTraits(emptyMap()),
        options = RudderOption(),
    )

    analytics.alias(
        newId = "Alias ID 1",
        previousId = "Explicit Previous User ID 1",
        options = RudderOption()
    )

    analytics.group(
        groupId = "Group at ${Date()}",
        traits = RudderTraits(emptyMap()),
        options = RudderOption()
    )
    LoggerAnalytics.debug("Message sent")
}
