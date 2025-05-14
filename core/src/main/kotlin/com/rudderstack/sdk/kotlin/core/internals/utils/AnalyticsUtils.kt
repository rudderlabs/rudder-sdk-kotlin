package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics

/**
 * Checks if the analytics instance is active.
 */
@InternalRudderApi
fun Analytics.isAnalyticsActive(): Boolean {
    if (isAnalyticsShutdown) {
        LoggerAnalytics.error("Analytics instance has been shutdown. No further operations are allowed.")
        return false
    }
    return true
}

/**
 * Checks if the source is enabled.
 */
@InternalRudderApi
fun Analytics.isSourceEnabled(): Boolean {
    if (!sourceConfigState.value.source.isSourceEnabled) {
        LoggerAnalytics.error("Source is disabled. This operation is not allowed.")
        return false
    }
    return true
}

/**
 * Marks the current write key as invalid and shuts down the analytics instance.
 * This method is typically invoked when the system determines that the write key cannot be used anymore.
 *
 * **NOTE: It deletes the stored events and preferences associated with the `writeKey`, and shuts down the SDK.**
 *
 * Treat this as a terminal operation, as once the SDK is shutdown no further operation will be allowed.
 */
@UseWithCaution
internal fun Analytics.handleInvalidWriteKey() {
    isInvalidWriteKey = true
    shutdown()
}
