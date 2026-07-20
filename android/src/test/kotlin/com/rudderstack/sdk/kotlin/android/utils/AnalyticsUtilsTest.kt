package com.rudderstack.sdk.kotlin.android.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsUtilsTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    @Test
    fun `given a previous job that is still running, when runOnAnalyticsThreadAfter is called, then the block does not run until the previous job completes`() =
        runTest(testDispatcher) {
            val executionOrder = mutableListOf<String>()
            val previousJob = mockAnalytics.runOnAnalyticsThread {
                delay(DELAY_IN_MILLIS)
                executionOrder.add(PREVIOUS)
            }

            mockAnalytics.runOnAnalyticsThreadAfter(previousJob) {
                executionOrder.add(AFTER)
            }

            // While the previous job is still suspended, the chained block must not have run.
            testDispatcher.scheduler.runCurrent()
            assertTrue(executionOrder.isEmpty())

            // Once the previous job completes, the chained block runs after it, preserving order.
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(listOf(PREVIOUS, AFTER), executionOrder)
        }

    @Test
    fun `given a null previous job, when runOnAnalyticsThreadAfter is called, then the block runs without waiting`() =
        runTest(testDispatcher) {
            val executionOrder = mutableListOf<String>()

            mockAnalytics.runOnAnalyticsThreadAfter(previousJob = null) {
                executionOrder.add(AFTER)
            }

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(listOf(AFTER), executionOrder)
        }

    @Test
    fun `given a chain of jobs where the first is slowest, when submitted in order, then they still run in submission order`() =
        runTest(testDispatcher) {
            val executionOrder = mutableListOf<String>()

            var lastJob = mockAnalytics.runOnAnalyticsThread {
                delay(DELAY_IN_MILLIS)
                executionOrder.add(FIRST)
            }
            lastJob = mockAnalytics.runOnAnalyticsThreadAfter(lastJob) {
                executionOrder.add(SECOND)
            }
            mockAnalytics.runOnAnalyticsThreadAfter(lastJob) {
                executionOrder.add(THIRD)
            }

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(listOf(FIRST, SECOND, THIRD), executionOrder)
        }

    companion object {

        private const val DELAY_IN_MILLIS = 10_000L
        private const val PREVIOUS = "previous"
        private const val AFTER = "after"
        private const val FIRST = "first"
        private const val SECOND = "second"
        private const val THIRD = "third"
    }
}
