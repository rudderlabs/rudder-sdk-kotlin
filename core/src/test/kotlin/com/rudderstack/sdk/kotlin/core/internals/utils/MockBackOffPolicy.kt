package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.internals.policies.backoff.BackOffPolicy

private const val DEFAULT_BACKOFF_DELAY = 0L

class MockBackOffPolicy(
    private val simulatedDelayInMillis: Long = DEFAULT_BACKOFF_DELAY,
) : BackOffPolicy {

    override fun nextDelayInMillis(): Long {
        return simulatedDelayInMillis
    }

    override fun resetBackOff() {
        // No-op
    }
}
