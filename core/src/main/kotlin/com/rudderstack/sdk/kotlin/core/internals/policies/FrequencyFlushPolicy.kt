package com.rudderstack.sdk.kotlin.core.internals.policies

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal const val DEFAULT_FLUSH_INTERVAL_IN_MILLIS = 10_000L
internal const val DEFAULT_MIN_SLEEP_TIMEOUT_IN_MILLIS = 1000L

/**
 * FrequencyFlushPolicy is a concrete implementation of the FlushPolicy interface
 * that automatically triggers a flush action at a specified interval, once scheduled.
 *
 * @property flushIntervalInMillis The interval in milliseconds between each flush.
 *                                 If set below [DEFAULT_MIN_SLEEP_TIMEOUT_IN_MILLIS], it defaults
 *                                 to [DEFAULT_FLUSH_INTERVAL_IN_MILLIS].
 */
class FrequencyFlushPolicy(private var flushIntervalInMillis: Long = DEFAULT_FLUSH_INTERVAL_IN_MILLIS) : FlushPolicy {

    private var flushJob: Job? = null
    private var jobStarted: Boolean = false
    private var logger: Logger? = null

    init {
        flushIntervalInMillis = when {
            flushIntervalInMillis >= DEFAULT_MIN_SLEEP_TIMEOUT_IN_MILLIS -> flushIntervalInMillis
            else -> DEFAULT_FLUSH_INTERVAL_IN_MILLIS
        }
    }

    internal fun schedule(analytics: Analytics) {
        if (!jobStarted) {
            jobStarted = true
            logger = analytics.logger
            analytics.logger.debug("FrequencyFlushPolicy: Scheduled flush every ${flushIntervalInMillis}ms")

            flushJob = analytics.analyticsScope.launch(analytics.analyticsDispatcher) {
                if (flushIntervalInMillis > 0) {
                    do {
                        delay(flushIntervalInMillis)
                        analytics.flush()
                    } while (isActive)
                }
            }
        }
    }

    internal fun cancelSchedule() {
        if (jobStarted) {
            logger?.debug("FrequencyFlushPolicy: Flush schedule cancelled")
            jobStarted = false
            flushJob?.cancel()
        }
    }
}
