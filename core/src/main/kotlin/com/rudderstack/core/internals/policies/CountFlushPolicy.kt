package com.rudderstack.core.internals.policies

const val DEFAULT_FLUSH_AT = 30

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
