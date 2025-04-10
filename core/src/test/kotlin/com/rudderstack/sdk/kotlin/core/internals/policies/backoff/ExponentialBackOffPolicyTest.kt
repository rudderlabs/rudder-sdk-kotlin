package com.rudderstack.sdk.kotlin.core.internals.policies.backoff

import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.security.SecureRandom

class ExponentialBackOffPolicyTest {

    @BeforeEach
    fun setUp() {
        mockkConstructor(SecureRandom::class)
        every { anyConstructed<SecureRandom>().nextInt(any()) } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `when nextDelayInMillis called, then its return value should grow exponentially with jitter`() {
        // mockking the jitter to be half of the delay
        every { anyConstructed<SecureRandom>().nextInt(any()) } answers { firstArg<Int>() / 2 }

        val policy = ExponentialBackOffPolicy(intervalInMillis = 1000L, base = 2.0)

        val delay1 = policy.nextDelayInMillis()
        val delay2 = policy.nextDelayInMillis()
        val delay3 = policy.nextDelayInMillis()

        assertEquals(1500L, delay1) // 1000 + 500 (jitter)
        assertEquals(3000L, delay2) // 2000 + 1000 (jitter)
        assertEquals(6000L, delay3) // 4000 + 2000 (jitter)
    }

    @Test
    fun `when resetBackOff called, then it should reset attempt counter`() {
        val policy = ExponentialBackOffPolicy(intervalInMillis = 500L, base = 2.0)

        policy.nextDelayInMillis() // attempt 0
        policy.nextDelayInMillis() // attempt 1
        policy.resetBackOff()
        val delayAfterReset = policy.nextDelayInMillis() // attempt should be back to 0

        assertEquals(500L, delayAfterReset)
    }

    @Test
    fun `when invalid interval passed, then exponential backoff should use default interval`() {
        val policy = ExponentialBackOffPolicy(intervalInMillis = 5L)
        val delay = policy.nextDelayInMillis()
        assertEquals(DEFAULT_INTERVAL, delay)
    }

    @Test
    fun `when invalid base passed, then it should use default base`() {
        val policy = ExponentialBackOffPolicy(base = 10.0)
        val firstDelay = policy.nextDelayInMillis()

        assertEquals(DEFAULT_INTERVAL, firstDelay)
    }

    @RepeatedTest(10)
    fun `when nextDelayInMillis called, then the jitter should not exceed the delay`() {
        // unmockking the SecureRandom to get the actual jitter
        unmockkAll()

        val policy = ExponentialBackOffPolicy(intervalInMillis = 1000L, base = 2.0)
        val delay = policy.nextDelayInMillis()

        val expectedMax = 1000L + 999L
        assertTrue(delay <= expectedMax)
    }
}
