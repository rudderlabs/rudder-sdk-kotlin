package com.rudderstack.sdk.kotlin.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope

/**
 * A clock implementation for testing that uses the TestCoroutineScheduler
 * to provide consistent time values during tests.
 */
class TestClock(private val scheduler: TestCoroutineScheduler) {
    /**
     * Returns the current virtual time from the test scheduler in milliseconds.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun currentTimeInMillis(): Long = scheduler.currentTime
}

/**
 * Extension function to create a TestClock from a TestScope.
 * Provides easy access to virtual time in coroutine tests.
 */
fun TestScope.createTestClock(): TestClock = TestClock(testScheduler)
