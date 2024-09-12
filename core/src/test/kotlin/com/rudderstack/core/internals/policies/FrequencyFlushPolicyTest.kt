package com.rudderstack.core.internals.policies

import com.rudderstack.core.internals.utils.mockAnalytics
import com.rudderstack.core.utils.advanceTimeBy
import io.mockk.coVerify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Test

class FrequencyFlushPolicyTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

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
