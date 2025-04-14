package com.rudderstack.sdk.kotlin.core.internals.policies.backoff

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.random.Random

class ExponentialBackOffPolicyTest {

    @BeforeEach
    fun setUp() {
        mockkObject(Random)
        every { Random.nextLong(any()) } returns 0L
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `when nextDelayInMillis called, then its return value should grow exponentially with jitter`() {
        // mockking the jitter to be half of the delay
        every { Random.nextLong(any()) } answers { firstArg<Long>() / 2 }

        val policy = ExponentialBackOffPolicy(minDelayInMillis = 3000L, base = 2.0)

        val delay1 = policy.nextDelayInMillis()
        val delay2 = policy.nextDelayInMillis()
        val delay3 = policy.nextDelayInMillis()

        assertEquals(4500L, delay1) // 3000 + 500 (jitter)
        assertEquals(9000L, delay2) // 6000 + 3000 (jitter)
        assertEquals(18000L, delay3) // 12000 + 6000 (jitter)
    }

    @Test
    fun `when resetBackOff called, then it should reset attempt counter`() {
        val policy = ExponentialBackOffPolicy(minDelayInMillis = 3000L, base = 2.0)

        policy.nextDelayInMillis() // attempt 0
        policy.nextDelayInMillis() // attempt 1
        policy.resetBackOff()
        val delayAfterReset = policy.nextDelayInMillis() // attempt should be back to 0

        assertEquals(3000L, delayAfterReset)
    }

    @Test
    fun `when invalid interval passed, then exponential backoff should use default interval`() {
        val policy = ExponentialBackOffPolicy(minDelayInMillis = 5L)
        val delay = policy.nextDelayInMillis()
        assertEquals(DEFAULT_INTERVAL_IN_MILLIS, delay)
    }

    @Test
    fun `when invalid base passed, then it should use default base`() {
        val policy = ExponentialBackOffPolicy(base = 10.0)
        val firstDelay = policy.nextDelayInMillis()

        assertEquals(DEFAULT_INTERVAL_IN_MILLIS, firstDelay)
    }

    @RepeatedTest(10)
    fun `when nextDelayInMillis called, then the jitter should not exceed the delay`() {
        // unmockking the Random to get the actual jitter
        unmockkAll()

        val policy = ExponentialBackOffPolicy(minDelayInMillis = 3000L, base = 2.0)
        val delay = policy.nextDelayInMillis()

        val expectedMax = 3000L + 2999L
        val expectedMin = 3000L
        assertTrue(delay in expectedMin..expectedMax) // delay should be between 3000 and 5999
    }
}
