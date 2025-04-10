package com.rudderstack.sdk.kotlin.core.internals.policies.backoff

import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi

/**
 * Interface representing a backoff policy for retrying operations.
 * Implementations should provide a strategy for calculating the next delay
 * and resetting the backoff state.
 */
@InternalRudderApi
interface BackOffPolicy {

    /**
     * Calculates the next delay in milliseconds based on the backoff policy.
     *
     * @return The next delay in milliseconds.
     */
    fun nextDelayInMillis(): Long

    /**
     * Resets the backoff policy to its initial state.
     * This method should be called when the backoff policy needs to be restarted.
     */
    fun resetBackOff()
}
