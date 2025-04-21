package com.rudderstack.sdk.kotlin.core.internals.policies.backoff

import com.rudderstack.sdk.kotlin.core.createTestClock
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@ExperimentalCoroutinesApi
class MaxAttemptsExponentialBackoffTest {

    private val mockBackOffPolicy = mockk<BackOffPolicy>()
    private val maxAttempts = 3
    private val coolOffPeriod = 30.minutes
    private lateinit var backoff: MaxAttemptsExponentialBackoff
    private val expectedDelays = listOf(3000L, 5000L, 9000L)

    @BeforeEach
    fun setup() {
        backoff = MaxAttemptsExponentialBackoff(
            maxAttempts = maxAttempts,
            coolOffPeriod = coolOffPeriod,
            exponentialBackOffPolicy = mockBackOffPolicy
        )

        every { mockBackOffPolicy.resetBackOff() } returns Unit

        every { mockBackOffPolicy.nextDelayInMillis() } returnsMany expectedDelays
    }

    @Test
    fun `when delayWithBackoff called, then applies correct delay`() = runTest {
        expectedDelays.forEachIndexed { index, expectedDelay ->
            val attemptNumber = index + 1
            val timeBefore = createTestClock().currentTimeInMillis()

            backoff.delayWithBackoff()

            val timeAfter = createTestClock().currentTimeInMillis()
            assertEquals(timeBefore + expectedDelay, timeAfter, "Failed on attempt #$attemptNumber")
            verify(exactly = attemptNumber) { mockBackOffPolicy.nextDelayInMillis() }
        }

        confirmVerified(mockBackOffPolicy)
    }

    @Test
    fun `given max attempts reached, when delayWithBackoff called, then applies cool-off period and resets`() = runTest {
        // Go through all normal attempts
        repeat(maxAttempts) {
            backoff.delayWithBackoff()
            advanceTimeBy(expectedDelays[it])
        }
        val timeBefore = createTestClock().currentTimeInMillis()

        // One more attempt that should trigger cool-off
        backoff.delayWithBackoff()

        // Should have reset and applied cool-off period
        val timeAfter = createTestClock().currentTimeInMillis()
        assertEquals(timeBefore + coolOffPeriod.inWholeMilliseconds, timeAfter, "Failed on attempt #${maxAttempts + 1}")
        verify { mockBackOffPolicy.resetBackOff() }
        verify(exactly = maxAttempts) { mockBackOffPolicy.nextDelayInMillis() }

        confirmVerified(mockBackOffPolicy)
    }

    @Test
    fun `when reset called, then resets attempts counter and backoff policy`() = runTest {
        // Go through all normal attempts
        repeat(maxAttempts) {
            backoff.delayWithBackoff()
            advanceTimeBy(expectedDelays[it])
        }

        backoff.reset()

        verify { mockBackOffPolicy.resetBackOff() }

        // Now check that the counter was reset by making another attempt
        // and verifying we're back to the first delay
        val timeBefore = createTestClock().currentTimeInMillis()
        every { mockBackOffPolicy.nextDelayInMillis() } returnsMany expectedDelays

        backoff.delayWithBackoff()

        val timeAfter = createTestClock().currentTimeInMillis()
        // Should have applied the first delay again
        assertEquals(timeBefore + expectedDelays[0], timeAfter, "Failed on attempt #${maxAttempts + 1}")
        verify(exactly = maxAttempts + 1) { mockBackOffPolicy.nextDelayInMillis() }

        confirmVerified(mockBackOffPolicy)
    }
}
