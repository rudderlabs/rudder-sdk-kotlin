package com.rudderstack.sdk.kotlin.core.internals.policies.backoff

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import kotlinx.coroutines.delay

private const val DEFAULT_MAX_ATTEMPT = 5
private const val DEFAULT_COOL_OFF_PERIOD_IN_MILLIS = 30 * 60 * 1000L // 30 minutes

/**
 * Manages retry attempts with exponential backoff delays.
 *
 * This class:
 * - Applies increasing delays between retry attempts
 * - Tracks attempt count and enforces maximum limits
 * - Suspends execution between attempts with appropriate delays
 * - Implements a longer cool-off period when max attempts are reached
 *
 * @param maxAttempts Maximum retries before entering cool-off (default: 5)
 * @param coolOffPeriodInMillis Duration in ms after max attempts (default: 30 minutes)
 * @param base Exponential factor for backoff calculation (default: 2.0)
 * @param minDelayInMillis Initial delay in milliseconds (default: 3000ms)
 * @param exponentialBackOffPolicy Delay calculation policy
 */
internal class MaxAttemptsExponentialBackoff(
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPT,
    private val coolOffPeriodInMillis: Long = DEFAULT_COOL_OFF_PERIOD_IN_MILLIS,
    base: Double = DEFAULT_BASE,
    minDelayInMillis: Long = DEFAULT_INTERVAL_IN_MILLIS,
    private val exponentialBackOffPolicy: BackOffPolicy = ExponentialBackOffPolicy(
        minDelayInMillis = minDelayInMillis,
        base = base,
    ),
) {

    private var consecutiveAttempts = 0

    /**
     * Calculates and applies the appropriate delay between retry attempts.
     *
     * This method:
     * 1. Increments the consecutive attempts counter
     * 2. If max attempts exceeded: resets counters and applies cool-off period
     * 3. Otherwise: suspends execution for the calculated delay
     *
     * The delay increases with each attempt according to the
     * configured backoff policy until reaching max attempts.
     */
    internal suspend fun delayWithBackoff() {
        consecutiveAttempts++
        when {
            consecutiveAttempts > maxAttempts -> {
                LoggerAnalytics.verbose("Max attempts reached. Entering cool-off period for upload queue")
                reset()
                LoggerAnalytics.verbose("Next attempt will be after $coolOffPeriodInMillis milliseconds")
                delay(coolOffPeriodInMillis)
            }

            else -> {
                exponentialBackOffPolicy.nextDelayInMillis().let { delayTime ->
                    LoggerAnalytics.verbose(
                        "Sleeping for $delayTime milliseconds (attempt $consecutiveAttempts of $maxAttempts)"
                    )
                    delay(delayTime)
                }
            }
        }
    }

    /**
     * Resets the backoff policy to its initial state.
     */
    internal fun reset() {
        LoggerAnalytics.verbose("Resetting retry attempts and backoff policy")
        consecutiveAttempts = 0
        exponentialBackOffPolicy.resetBackOff()
    }
}
