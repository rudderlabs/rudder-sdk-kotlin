package internals.policies

import com.rudderstack.core.internals.policies.CountFlushPolicy
import com.rudderstack.core.internals.policies.DEFAULT_FLUSH_AT
import com.rudderstack.core.internals.policies.DEFAULT_FLUSH_INTERVAL_IN_MILLIS
import com.rudderstack.core.internals.policies.FlushPoliciesFacade
import com.rudderstack.core.internals.policies.FrequencyFlushPolicy
import com.rudderstack.core.internals.policies.StartupFlushPolicy
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import utils.mockAnalytics

class FlushPoliciesTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    @Test
    fun `given only StartupFlushPolicy is enabled, when shouldFlush is called, then it should return true`() {
        val startupFlushPolicy = StartupFlushPolicy()
        val flushPoliciesFacade = FlushPoliciesFacade(
            listOf(startupFlushPolicy)
        )

        assertTrue(flushPoliciesFacade.shouldFlush())
    }

    @Test
    fun `given only CountFlushPolicy is enabled, when default number of events have been tracked, then shouldFlush should return true`() {
        val countFlushPolicy = CountFlushPolicy()
        val flushPoliciesFacade = FlushPoliciesFacade(
            listOf(countFlushPolicy)
        )

        // Until the default number of events have been tracked, it should not flush
        assertFalse(flushPoliciesFacade.shouldFlush())

        repeat(DEFAULT_FLUSH_AT) {
            flushPoliciesFacade.updateState()
        }

        assertTrue(flushPoliciesFacade.shouldFlush())
    }

    @Test
    fun `given only FrequencyFlushPolicy is enabled, when the policy is scheduled, then it should flush after the default interval`() {
        val frequencyFlushPolicy = FrequencyFlushPolicy()
        val flushPoliciesFacade = FlushPoliciesFacade(
            listOf(frequencyFlushPolicy)
        )

        flushPoliciesFacade.schedule(mockAnalytics)

        advanceTimeBy(DEFAULT_FLUSH_INTERVAL_IN_MILLIS)
        coVerify(exactly = 1) {
            mockAnalytics.flush()
        }
    }

    @Test
    fun `given both StartupFlushPolicy and CountFlushPolicy are enabled, when at starting shouldFlush is called and after default number of events have been tracked, then it should return true`() {
        val startupFlushPolicy = StartupFlushPolicy()
        val countFlushPolicy = CountFlushPolicy()
        val flushPoliciesFacade = FlushPoliciesFacade(
            listOf(startupFlushPolicy, countFlushPolicy)
        )

        // At starting, it should flush
        assertTrue(flushPoliciesFacade.shouldFlush())
        // It should not flush until the default number of events have been tracked
        assertFalse(flushPoliciesFacade.shouldFlush())

        repeat(DEFAULT_FLUSH_AT) {
            flushPoliciesFacade.updateState()
        }

        assertTrue(flushPoliciesFacade.shouldFlush())
    }

    @Test
    fun `given both StartupFlushPolicy and FrequencyFlushPolicy are enabled, when at starting shouldFlush is called and after the default interval, then it should return true and call flush`() {
        val startupFlushPolicy = StartupFlushPolicy()
        val frequencyFlushPolicy = FrequencyFlushPolicy()
        val flushPoliciesFacade = FlushPoliciesFacade(
            listOf(startupFlushPolicy, frequencyFlushPolicy)
        )

        flushPoliciesFacade.schedule(mockAnalytics)

        // At starting, it should flush
        assertTrue(flushPoliciesFacade.shouldFlush())
        // It should not flush before the default interval
        assertFalse(flushPoliciesFacade.shouldFlush())

        advanceTimeBy(DEFAULT_FLUSH_INTERVAL_IN_MILLIS)
        coVerify(exactly = 1) {
            mockAnalytics.flush()
        }
    }

    @Test
    fun `given all three policies are enabled, when appropriate conditions are met, then it should flush and shouldFlush should return true`() {
        val startupFlushPolicy = StartupFlushPolicy()
        val countFlushPolicy = CountFlushPolicy()
        val frequencyFlushPolicy = FrequencyFlushPolicy()
        val flushPoliciesFacade = FlushPoliciesFacade(
            listOf(startupFlushPolicy, countFlushPolicy, frequencyFlushPolicy)
        )

        flushPoliciesFacade.schedule(mockAnalytics)

        // StartupFlushPolicy should flush at starting
        assertTrue(flushPoliciesFacade.shouldFlush())
        // Flush should return false until other conditions are met
        assertFalse(flushPoliciesFacade.shouldFlush())


        repeat(DEFAULT_FLUSH_AT) {
            flushPoliciesFacade.updateState()
        }

        // CountFlushPolicy should flush after the default number of events have been tracked
        assertTrue(flushPoliciesFacade.shouldFlush())

        // Reset the count of CountFlushPolicy
        flushPoliciesFacade.reset()
        assertFalse(flushPoliciesFacade.shouldFlush())

        // FrequencyFlushPolicy should flush after the default interval
        advanceTimeBy(DEFAULT_FLUSH_INTERVAL_IN_MILLIS)
        coVerify(exactly = 1) {
            mockAnalytics.flush()
        }

        // Cancel the scheduler of FrequencyFlushPolicy
        flushPoliciesFacade.cancelSchedule()

        advanceTimeBy(DEFAULT_FLUSH_INTERVAL_IN_MILLIS)
        coVerify(exactly = 1) {
            mockAnalytics.flush()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun advanceTimeBy(timeInMillis: Long) {
        testDispatcher.scheduler.advanceTimeBy(timeInMillis)
        testDispatcher.scheduler.runCurrent()
    }
}
