package com.rudderstack.core.internals.policies

import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.utils.ExecutionManager.runOnce
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

const val DEFAULT_FLUSH_INTERVAL_IN_MILLIS = 10_000L
const val DEFAULT_MIN_SLEEP_TIMEOUT = 1L

class FrequencyFlushPolicy(private var flushIntervalInMillis: Long = DEFAULT_FLUSH_INTERVAL_IN_MILLIS) : FlushPolicy {

    private var flushJob: Job? = null

    init {
        flushIntervalInMillis = when {
            flushIntervalInMillis >= DEFAULT_MIN_SLEEP_TIMEOUT -> flushIntervalInMillis
            else -> DEFAULT_FLUSH_INTERVAL_IN_MILLIS
        }
    }

    fun schedule(analytics: Analytics) {
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

    fun cancelSchedule() {
        flushJob?.cancel()
    }
}
