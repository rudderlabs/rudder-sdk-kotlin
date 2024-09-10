package com.rudderstack.core.internals.policies

import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.utils.ExecutionManager.runOnce
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal const val DEFAULT_FLUSH_INTERVAL_IN_MILLIS = 10_000L
internal const val DEFAULT_MIN_SLEEP_TIMEOUT = 1L

/**
 * FrequencyFlushPolicy is a concrete implementation of the FlushPolicy interface
 * that automatically triggers a flush action at a specified interval, once scheduled.
 *
 * @property flushIntervalInMillis The interval in milliseconds between each flush.
 *                                 If set below [DEFAULT_MIN_SLEEP_TIMEOUT], it defaults
 *                                 to [DEFAULT_FLUSH_INTERVAL_IN_MILLIS].
 */
class FrequencyFlushPolicy(private var flushIntervalInMillis: Long = DEFAULT_FLUSH_INTERVAL_IN_MILLIS) : FlushPolicy {

    private var flushJob: Job? = null

    init {
        flushIntervalInMillis = when {
            flushIntervalInMillis >= DEFAULT_MIN_SLEEP_TIMEOUT -> flushIntervalInMillis
            else -> DEFAULT_FLUSH_INTERVAL_IN_MILLIS
        }
    }

    internal fun schedule(analytics: Analytics) {
        runOnce {
            flushJob = analytics.analyticsScope.launch(analytics.storageDispatcher) {
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
        flushJob?.cancel()
    }
}
