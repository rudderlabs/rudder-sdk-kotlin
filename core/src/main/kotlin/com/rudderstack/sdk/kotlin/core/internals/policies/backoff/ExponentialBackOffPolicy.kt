package com.rudderstack.sdk.kotlin.core.internals.policies.backoff

import java.security.SecureRandom
import kotlin.math.pow

/**
 * This class implements an exponential backoff strategy with jitter for handling retries.
 * It allows for configurable interval and base for the exponential calculation.
 * The next delay is calculated using the formula: delay = interval * base^attempt.
 * The delay is then adjusted with a random jitter to avoid synchronized retries.
 */
class ExponentialBackOffPolicy(
    private val intervalInMillis: Long,
    private val base: Double = 2.0,
) : BackOffPolicy {
    private var attempt = 0
    private val random = SecureRandom()

    override fun nextDelayInMillis(): Long {
        val delayInMillis = (intervalInMillis * base.pow(attempt++)).toLong()
        val delayWithJitterInMillis = withJitter(delayInMillis)

        return delayWithJitterInMillis
    }

    private fun withJitter(delayInMillis: Long): Long {
        val jitter = random.nextInt(delayInMillis.toInt())
        return delayInMillis + jitter
    }

    override fun resetBackOff() {
        attempt = 0
    }
}
