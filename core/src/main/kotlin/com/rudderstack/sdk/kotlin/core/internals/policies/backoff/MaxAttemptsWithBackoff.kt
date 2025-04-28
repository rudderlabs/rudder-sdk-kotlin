package com.rudderstack.sdk.kotlin.core.internals.policies.backoff

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private const val DEFAULT_MAX_ATTEMPT = 5
private val DEFAULT_COOL_OFF_PERIOD_IN_MILLIS = 30.minutes

/**
 * Manages retry attempts with backoff delays.
 *
 * This class:
 * - Applies increasing delays between retry attempts
 * - Tracks attempt count and enforces maximum limits
 * - Suspends execution between attempts with appropriate delays
 * - Implements a longer cool-off period when max attempts are reached
 *
 * @param maxAttempts Maximum retries before entering cool-off (default: 5)
 * @param coolOffPeriod Duration in minutes (default: 30 minutes)
 * @param backOffPolicy Delay calculation policy. Default to [ExponentialBackOffPolicy]
 */
internal class MaxAttemptsWithBackoff(
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPT,
    private val coolOffPeriod: Duration = DEFAULT_COOL_OFF_PERIOD_IN_MILLIS,
    private val backOffPolicy: BackOffPolicy = ExponentialBackOffPolicy(),
) {

    private var currentAttempt = 0

    internal suspend fun delayWithBackoff() {
        currentAttempt++
        when {
            currentAttempt > maxAttempts -> applyCoolOffPeriod()
            else -> applyBackoff()
        }
    }

    private suspend fun applyCoolOffPeriod() {
        LoggerAnalytics.verbose("Max attempts reached. Entering cool-off period for upload queue")
        reset()
        LoggerAnalytics.verbose("Next attempt will be after $coolOffPeriod")
        delay(coolOffPeriod)
    }

    private suspend fun applyBackoff() {
        val delayInMillis = backOffPolicy.nextDelayInMillis()
        LoggerAnalytics.verbose("Sleeping for $delayInMillis milliseconds (attempt $currentAttempt of $maxAttempts)")
        delay(delayInMillis)
    }

    internal fun reset() {
        LoggerAnalytics.verbose("Resetting retry attempts and backoff policy")
        currentAttempt = 0
        backOffPolicy.resetBackOff()
    }
}
