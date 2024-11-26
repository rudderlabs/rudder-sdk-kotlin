package com.rudderstack.kotlin.sdk.internals.utils

import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.logger.LoggerAnalytics

/**
 * Checks if the analytics instance is active.
 */
fun Analytics.isAnalyticsActive(): Boolean {
    if (isAnalyticsShutdown) {
        LoggerAnalytics.error("Analytics instance has been shutdown. No further operations are allowed.")
        return false
    }
    return true
}
