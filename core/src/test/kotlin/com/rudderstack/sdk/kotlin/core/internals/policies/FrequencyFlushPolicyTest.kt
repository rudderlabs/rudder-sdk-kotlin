package com.rudderstack.sdk.kotlin.core.internals.policies

import com.rudderstack.sdk.kotlin.core.advanceTimeBy
import io.mockk.coVerify
import kotlinx.coroutines.test.StandardTestDispatcher
import com.rudderstack.sdk.kotlin.core.mockAnalytics
import io.mockk.every
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FrequencyFlushPolicyTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    @BeforeEach
    fun setup() {
        every { mockAnalytics.isSourceEnabled } returns true
    }

    @Test
    fun `given the flush interval is set to the default value, when the policy is scheduled, then it should flush after the default interval`() {
        val frequencyFlushPolicy = FrequencyFlushPolicy()

        frequencyFlushPolicy.schedule(mockAnalytics)

        testDispatcher.advanceTimeBy()
        coVerify(exactly = 1) {
            mockAnalytics.flush()
        }
    }

    @Test
    fun `given the flush interval is set to the default value, when the policy is scheduled, then it should not flush before the default interval`() {
        val frequencyFlushPolicy = FrequencyFlushPolicy()

        frequencyFlushPolicy.schedule(mockAnalytics)

        testDispatcher.advanceTimeBy(DEFAULT_FLUSH_INTERVAL_IN_MILLIS - 1)
        coVerify(exactly = 0) {
            mockAnalytics.flush()
        }
    }

    @Test
    fun `given flush interval is set to the default value, when the policy is scheduled, then it should keep on flushing until cancelled`() {
        val frequencyFlushPolicy = FrequencyFlushPolicy()

        frequencyFlushPolicy.schedule(mockAnalytics)

        // After the first interval, it should flush
        testDispatcher.advanceTimeBy()
        coVerify(exactly = 1) {
            mockAnalytics.flush()
        }

        // After the second interval, it should flush again
        testDispatcher.advanceTimeBy()
        coVerify(exactly = 2) {
            mockAnalytics.flush()
        }

        // After the third interval, it should flush again
        testDispatcher.advanceTimeBy()
        coVerify(exactly = 3) {
            mockAnalytics.flush()
        }

        // Cancel the scheduler
        frequencyFlushPolicy.cancelSchedule()

        testDispatcher.advanceTimeBy()
        // Should not flush after cancelling
        coVerify(exactly = 3) {
            mockAnalytics.flush()
        }
    }

    @Test
    fun `given a flush interval, when the policy is scheduled but the source is disabled, then it should not attempt flush`() =
        runTest {
            val frequencyFlushPolicy = FrequencyFlushPolicy()

            every { mockAnalytics.isSourceEnabled } returns false
            frequencyFlushPolicy.schedule(mockAnalytics)

            testDispatcher.advanceTimeBy()
            coVerify(exactly = 0) {
                mockAnalytics.flush()
            }
        }

    @Test
    fun `given a flush interval, when the policy is schedule and source is re-enabled, the flushing should resume`() = runTest {
        val frequencyFlushPolicy = FrequencyFlushPolicy()

        every { mockAnalytics.isSourceEnabled } returns false
        frequencyFlushPolicy.schedule(mockAnalytics)

        testDispatcher.advanceTimeBy()
        coVerify(exactly = 0) {
            mockAnalytics.flush()
        }

        every { mockAnalytics.isSourceEnabled } returns true
        testDispatcher.advanceTimeBy()
        coVerify(exactly = 1) {
            mockAnalytics.flush()
        }
    }

    @Test
    fun `given flush interval is set to 1, when the policy is scheduled, then it should flush after the scheduled interval`() {
        val flushInterval = 1L
        val frequencyFlushPolicy = FrequencyFlushPolicy(flushInterval)

        frequencyFlushPolicy.schedule(mockAnalytics)

        testDispatcher.advanceTimeBy(flushInterval)
        coVerify(exactly = 1) {
            mockAnalytics.flush()
        }
    }

    @Test
    fun `given flush interval is set to 100s, when the policy is scheduled, then it should flush after the scheduled interval`() {
        val flushInterval = 100_000L
        val frequencyFlushPolicy = FrequencyFlushPolicy(flushInterval)

        frequencyFlushPolicy.schedule(mockAnalytics)

        testDispatcher.advanceTimeBy(flushInterval)
        coVerify(exactly = 1) {
            mockAnalytics.flush()
        }
    }

    @Test
    fun `given flush interval is set to 0, when the policy is scheduled, then it should flush after the default interval`() {
        val flushInterval = 0L
        val frequencyFlushPolicy = FrequencyFlushPolicy(flushInterval)

        frequencyFlushPolicy.schedule(mockAnalytics)

        testDispatcher.advanceTimeBy()
        coVerify(exactly = 1) {
            mockAnalytics.flush()
        }
    }

    @Test
    fun `given flush is cancelled just after scheduling, when the policy is scheduled, then it should not flush`() {
        val frequencyFlushPolicy = FrequencyFlushPolicy()

        frequencyFlushPolicy.schedule(mockAnalytics)
        frequencyFlushPolicy.cancelSchedule()

        testDispatcher.advanceTimeBy()
        coVerify(exactly = 0) {
            mockAnalytics.flush()
        }
    }

    @Test
    fun `given this policy is enabled, when scheduled is called multiple times, then it should flush only once after each interval until cancelled`() {
        val frequencyFlushPolicy = FrequencyFlushPolicy()

        frequencyFlushPolicy.schedule(mockAnalytics)
        frequencyFlushPolicy.schedule(mockAnalytics)
        frequencyFlushPolicy.schedule(mockAnalytics)

        testDispatcher.advanceTimeBy()
        coVerify(exactly = 1) {
            mockAnalytics.flush()
        }

        // After the second interval, it should flush again only once
        testDispatcher.advanceTimeBy()
        coVerify(exactly = 2) {
            mockAnalytics.flush()
        }

        // Cancel the scheduler
        frequencyFlushPolicy.cancelSchedule()

        testDispatcher.advanceTimeBy()
        // Should not flush after cancelling
        coVerify(exactly = 2) {
            mockAnalytics.flush()
        }
    }
}
