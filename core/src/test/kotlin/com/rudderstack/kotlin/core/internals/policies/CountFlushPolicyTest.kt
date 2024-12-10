package com.rudderstack.kotlin.core.internals.policies

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class CountFlushPolicyTest {

    @Test
    fun `given no event has been tracked, when shouldFlush is called, then it should return false`() {
        val countFlushPolicy = CountFlushPolicy()

        assertFalse(countFlushPolicy.shouldFlush())
    }

    @Test
    fun `given 10 events have been tracked, when shouldFlush is called, then it should return false`() {
        val countFlushPolicy = CountFlushPolicy()

        repeat(10) {
            countFlushPolicy.updateState()
        }

        assertFalse(countFlushPolicy.shouldFlush())
    }

    @Test
    fun `given the default number of events have been tracked, when shouldFlush is called, then it should return true`() {
        val countFlushPolicy = CountFlushPolicy()

        repeat(DEFAULT_FLUSH_AT) {
            countFlushPolicy.updateState()
        }

        assertTrue(countFlushPolicy.shouldFlush())
    }

    @Test
    fun `given flushAt is set to 1, when 1 events have been tracked, then shouldFlush should return true`() {
        val countFlushPolicy = CountFlushPolicy(1)

        repeat(1) {
            countFlushPolicy.updateState()
        }

        assertTrue(countFlushPolicy.shouldFlush())
    }

    @Test
    fun `given flushAt is set to 10, when 10 events have been tracked, then shouldFlush should return true`() {
        val countFlushPolicy = CountFlushPolicy(10)

        repeat(10) {
            countFlushPolicy.updateState()
        }

        assertTrue(countFlushPolicy.shouldFlush())
    }

    @Test
    fun `given 100 events have been tracked, when shouldFlush is called, then it should return true`() {
        val countFlushPolicy = CountFlushPolicy(100)

        repeat(100) {
            countFlushPolicy.updateState()
        }

        assertTrue(countFlushPolicy.shouldFlush())
    }

    @Test
    fun `given flushAt is set to -1, when the default number of events have been tracked, then shouldFlush should return true`() {
        val countFlushPolicy = CountFlushPolicy(-1)

        repeat(DEFAULT_FLUSH_AT) {
            countFlushPolicy.updateState()
        }

        assertTrue(countFlushPolicy.shouldFlush())
    }

    @Test
    fun `given flushAt is set to 101, when the default number of events have been tracked, then shouldFlush should return false`() {
        val countFlushPolicy = CountFlushPolicy(101)

        repeat(DEFAULT_FLUSH_AT) {
            countFlushPolicy.updateState()
        }

        assertTrue(countFlushPolicy.shouldFlush())
    }

    @Test
    fun `given this policy is enabled, when reset is called, then it should reset the count`() {
        val countFlushPolicy = CountFlushPolicy()

        repeat(DEFAULT_FLUSH_AT) {
            countFlushPolicy.updateState()
        }

        // It should flush now
        assertTrue(countFlushPolicy.shouldFlush())

        // Reset the counter
        countFlushPolicy.reset()

        // Now the counter should be reset
        assertFalse(countFlushPolicy.shouldFlush())
    }
}
