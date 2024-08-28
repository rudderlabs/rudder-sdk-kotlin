package com.rudderstack.core.internals.policies

const val DEFAULT_FLUSH_AT = 30

/**
 * CountFlushPolicy is a concrete implementation of the FlushPolicy interface
 * that triggers a flush action based on a predefined count threshold.
 *
 * @property flushAt The threshold count at which a flush should be triggered.
 *                   It defaults to [DEFAULT_FLUSH_AT] if not specified or if
 *                   an invalid value is provided.
 */
class CountFlushPolicy(private var flushAt: Int = DEFAULT_FLUSH_AT) : FlushPolicy {

    private var count: Int = 0

    init {
        flushAt = when {
            flushAt in 1..100 -> flushAt
            else -> DEFAULT_FLUSH_AT
        }
    }

    fun shouldFlush(): Boolean {
        return count >= flushAt
    }

    fun updateState() {
        count++
    }

    fun reset() {
        count = 0
    }
}
