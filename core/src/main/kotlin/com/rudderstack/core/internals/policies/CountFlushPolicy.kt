package com.rudderstack.core.internals.policies

internal const val DEFAULT_FLUSH_AT = 30
private const val MIN_VALUE = 1
private const val MAX_VALUE = 100

/**
 * CountFlushPolicy is a concrete implementation of the FlushPolicy interface
 * that indicates if flush action should be trigger based on a predefined count threshold.
 *
 * @property flushAt The threshold count at which a flush should be triggered.
 *                   It defaults to [DEFAULT_FLUSH_AT] if not specified or if
 *                   an invalid value is provided.
 */
class CountFlushPolicy(private var flushAt: Int = DEFAULT_FLUSH_AT) : FlushPolicy {

    private var count: Int = 0

    init {
        flushAt = when {
            flushAt in MIN_VALUE..MAX_VALUE -> flushAt
            else -> DEFAULT_FLUSH_AT
        }
    }

    internal fun shouldFlush(): Boolean {
        return count >= flushAt
    }

    internal fun updateState() {
        count++
    }

    internal fun reset() {
        count = 0
    }
}
