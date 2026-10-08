package com.rudderstack.sdk.kotlin.core.server

import com.rudderstack.sdk.kotlin.core.Analytics

/**
 * Receives a report each time the SDK drops events in server mode.
 *
 * The SDK calls the listener on an SDK thread. Do not block in [onDrop].
 * The SDK catches and logs an exception that [onDrop] throws.
 */
fun interface DropListener {

    /**
     * Reports a drop.
     *
     * @param reason The reason for the drop.
     * @param eventCount The number of dropped events. The value is 0 when the SDK cannot read the dropped batch.
     */
    fun onDrop(reason: DropReason, eventCount: Int)
}

/**
 * The reason for a drop that a [DropListener] receives.
 */
enum class DropReason {

    /** The queue holds [ServerConfiguration.maxQueuedEvents] events, so the SDK dropped a new event. */
    QUEUE_FULL,

    /** The upload of a batch failed after [ServerConfiguration.maxRetries] retries. */
    RETRIES_EXHAUSTED,

    /** The data plane rejected a batch with a status code that a retry cannot fix (400, 404, or 413). */
    REJECTED_BY_SERVER,

    /** An event call had a blank `userId` and a blank `anonymousId`. */
    MISSING_IDENTITY,
}

@Suppress("TooGenericExceptionCaught")
internal fun Analytics.reportDrop(reason: DropReason, eventCount: Int) {
    val dropListener = configuration.asServerConfigurationOrNull()?.dropListener ?: return
    try {
        dropListener.onDrop(reason, eventCount)
    } catch (e: Exception) {
        logger.error("DropListener: The listener threw an exception for $reason", e)
    }
}
