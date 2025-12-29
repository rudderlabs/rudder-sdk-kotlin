package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType

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
 * Checks if the source is enabled for the current platform.
 * For mobile platforms, returns the source enabled state from configuration.
 * For non-mobile platforms, always returns true.
 *
 * @return true if source is enabled or platform is not mobile, false otherwise
 */
@InternalRudderApi
fun Analytics.isSourceEnabled(): Boolean {
    if (getPlatformType() == PlatformType.Mobile) {
        return sourceConfigState.value.source.isSourceEnabled
    }
    return true
}

/**
 * Checks if the source is enabled and logs an error if it's disabled.
 * This method should be used when you want to validate the source state
 * and provide diagnostic logging.
 *
 * @return true if source is enabled or platform is not mobile, false otherwise
 */
@InternalRudderApi
fun Analytics.isSourceEnabledWithLogging(): Boolean {
    if (!isSourceEnabled()) {
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

/**
 * Disables the source for mobile platforms, preventing any events from being sent to RudderStack.
 * This operation only applies to mobile platforms and has no effect on other platform types.
 *
 * When the source is disabled, the SDK will reject subsequent tracking operations.
 */
internal fun Analytics.disableSource() {
    if (getPlatformType() == PlatformType.Mobile) {
        sourceConfigState.dispatch(SourceConfig.DisableSourceAction())
    }
}
