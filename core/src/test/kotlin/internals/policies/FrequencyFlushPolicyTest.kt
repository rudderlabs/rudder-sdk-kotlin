package internals.policies

import com.rudderstack.core.internals.policies.DEFAULT_FLUSH_INTERVAL_IN_MILLIS
import com.rudderstack.core.internals.policies.FrequencyFlushPolicy
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import utils.mockAnalytics

class FrequencyFlushPolicyTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    @Test
    fun `given the flush interval is set to the default value, when the policy is scheduled, then it should flush after the default interval`() {
        val frequencyFlushPolicy = FrequencyFlushPolicy()

        frequencyFlushPolicy.schedule(mockAnalytics)

        advanceTimeBy(DEFAULT_FLUSH_INTERVAL_IN_MILLIS)
        coVerify(exactly = 1) {
            mockAnalytics.flush()
        }
    }

    @Test
    fun `given the flush interval is set to the default value, when the policy is scheduled, then it should not flush before the default interval`() {
        val frequencyFlushPolicy = FrequencyFlushPolicy()

        frequencyFlushPolicy.schedule(mockAnalytics)

        advanceTimeBy(DEFAULT_FLUSH_INTERVAL_IN_MILLIS - 1)
        coVerify(exactly = 0) {
            mockAnalytics.flush()
        }
    }

    @Test
    fun `given flush interval is set to the default value, when the policy is scheduled, then it should keep on flushing until cancelled`() {
        val frequencyFlushPolicy = FrequencyFlushPolicy()

        frequencyFlushPolicy.schedule(mockAnalytics)

        // After the first interval, it should flush
        advanceTimeBy(DEFAULT_FLUSH_INTERVAL_IN_MILLIS)
        coVerify(exactly = 1) {
            mockAnalytics.flush()
        }

        // After the second interval, it should flush again
        advanceTimeBy(DEFAULT_FLUSH_INTERVAL_IN_MILLIS)
        coVerify(exactly = 2) {
            mockAnalytics.flush()
        }

        // After the third interval, it should flush again
        advanceTimeBy(DEFAULT_FLUSH_INTERVAL_IN_MILLIS)
        coVerify(exactly = 3) {
            mockAnalytics.flush()
        }

        // Cancel the scheduler
        frequencyFlushPolicy.cancelSchedule()

        advanceTimeBy(DEFAULT_FLUSH_INTERVAL_IN_MILLIS)
        // Should not flush after cancelling
        coVerify(exactly = 3) {
            mockAnalytics.flush()
        }
    }

    @Test
    fun `given flush interval is set to 1, when the policy is scheduled, then it should flush after the scheduled interval`() {
        val frequencyFlushPolicy = FrequencyFlushPolicy(1)

        frequencyFlushPolicy.schedule(mockAnalytics)

        advanceTimeBy(1)
        coVerify(exactly = 1) {
            mockAnalytics.flush()
        }
    }

    @Test
    fun `given flush interval is set to 100s, when the policy is scheduled, then it should flush after the scheduled interval`() {
        val frequencyFlushPolicy = FrequencyFlushPolicy(100_000)

        frequencyFlushPolicy.schedule(mockAnalytics)

        advanceTimeBy(100_000)
        coVerify(exactly = 1) {
            mockAnalytics.flush()
        }
    }

    @Test
    fun `given flush interval is set to 0, when the policy is scheduled, then it should flush after the default interval`() {
        val frequencyFlushPolicy = FrequencyFlushPolicy(0)

        frequencyFlushPolicy.schedule(mockAnalytics)

        advanceTimeBy(DEFAULT_FLUSH_INTERVAL_IN_MILLIS)
        coVerify(exactly = 1) {
            mockAnalytics.flush()
        }
    }

    @Test
    fun `given flush is cancelled just after scheduling, when the policy is scheduled, then it should not flush`() {
        val frequencyFlushPolicy = FrequencyFlushPolicy()

        frequencyFlushPolicy.schedule(mockAnalytics)
        frequencyFlushPolicy.cancelSchedule()

        advanceTimeBy(DEFAULT_FLUSH_INTERVAL_IN_MILLIS)
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

        advanceTimeBy(DEFAULT_FLUSH_INTERVAL_IN_MILLIS)
        coVerify(exactly = 1) {
            mockAnalytics.flush()
        }

        // After the second interval, it should flush again only once
        advanceTimeBy(DEFAULT_FLUSH_INTERVAL_IN_MILLIS)
        coVerify(exactly = 2) {
            mockAnalytics.flush()
        }

        // Cancel the scheduler
        frequencyFlushPolicy.cancelSchedule()

        advanceTimeBy(DEFAULT_FLUSH_INTERVAL_IN_MILLIS)
        // Should not flush after cancelling
        coVerify(exactly = 2) {
            mockAnalytics.flush()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun advanceTimeBy(timeInMillis: Long) {
        testDispatcher.scheduler.advanceTimeBy(timeInMillis)
        testDispatcher.scheduler.runCurrent()
    }
}
