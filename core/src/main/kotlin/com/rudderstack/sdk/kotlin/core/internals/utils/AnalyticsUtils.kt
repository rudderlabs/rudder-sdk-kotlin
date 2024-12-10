package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics

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
