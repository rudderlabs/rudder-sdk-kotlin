package com.rudderstack.sdk.kotlin.core.internals.policies.backoff

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import kotlinx.coroutines.delay

private const val DEFAULT_COOL_OFF_PERIOD_IN_MILLIS = 30 * 60 * 1000L // 30 minutes

/**
 * Manages retry attempts with an exponential backoff strategy and enforces limits.
 *
 * Features:
 * - Tracks consecutive retry attempts
 * - Applies exponential backoff between retries
 * - Enforces a maximum attempt limit
 * - Implements a cool-off period when maximum attempts are reached
 * - Delegates actual backoff calculation to the provided BackOffPolicy
 *
 * @param maxAttempts Maximum retry attempts before entering cool-off (default: 5)
 * @param coolOffPeriod Duration in ms to pause after max attempts (default: 30 minutes)
 * @param base Exponential multiplier for backoff calculation (default: 2.0)
 * @param minDelayInMillis Initial delay in milliseconds (default: 3000ms)
 * @param exponentialBackOffPolicy The policy for calculating delay (default: ExponentialBackOffPolicy)
 */
internal class MaxAttemptsExponentialBackoff(
    private val maxAttempts: Int = 5,
    private val coolOffPeriod: Long = DEFAULT_COOL_OFF_PERIOD_IN_MILLIS,
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
                LoggerAnalytics.verbose("Next attempt will be after $coolOffPeriod milliseconds")
                delay(coolOffPeriod)
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
