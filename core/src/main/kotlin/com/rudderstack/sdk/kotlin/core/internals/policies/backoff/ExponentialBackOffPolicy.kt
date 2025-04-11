package com.rudderstack.sdk.kotlin.core.internals.policies.backoff

import java.security.SecureRandom
import kotlin.math.pow

internal const val MIN_MIN_DELAY_IN_MILLIS = 3000L
internal const val MAX_MIN_DELAY_IN_MILLIS = 60_000L
internal const val DEFAULT_INTERVAL_IN_MILLIS = 3000L

internal const val MIN_BASE = 2.0
internal const val MAX_BASE = 5.0
internal const val DEFAULT_BASE = 2.0

/**
 * This class implements an exponential backoff strategy with jitter for handling retries.
 * It allows for configurable interval and base for the exponential calculation.
 * The next delay is calculated using the formula: delay = interval * base^attempt.
 * The delay is then adjusted with a random jitter to avoid synchronized retries.
 */
internal class ExponentialBackOffPolicy(
    private var minDelayInMillis: Long = DEFAULT_INTERVAL_IN_MILLIS,
    private var base: Double = DEFAULT_BASE,
) : BackOffPolicy {
    private var attempt = 0
    private val random = SecureRandom()

    init {
        minDelayInMillis = when {
            minDelayInMillis in MIN_MIN_DELAY_IN_MILLIS..MAX_MIN_DELAY_IN_MILLIS -> minDelayInMillis
            else -> DEFAULT_INTERVAL_IN_MILLIS
        }
        base = when {
            base in MIN_BASE..MAX_BASE -> base
            else -> DEFAULT_BASE
        }
    }

    override fun nextDelayInMillis(): Long {
        val delayInMillis = (minDelayInMillis * base.pow(attempt++)).toLong()
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
