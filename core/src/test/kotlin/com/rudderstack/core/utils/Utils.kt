package com.rudderstack.core.utils

import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.policies.DEFAULT_FLUSH_INTERVAL_IN_MILLIS
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope

fun mockAnalytics(testScope: TestScope, testDispatcher: TestDispatcher): Analytics {
    val mockAnalytics = mockk<Analytics>(relaxed = true)

    mockAnalytics.also {
        every { it.analyticsScope } returns testScope
        every { it.analyticsDispatcher } returns testDispatcher
        every { it.storageDispatcher } returns testDispatcher
        every { it.networkDispatcher } returns testDispatcher
        every { it.mainDispatcher } returns testDispatcher
    }

    return mockAnalytics
}

@OptIn(ExperimentalCoroutinesApi::class)
fun TestDispatcher.advanceTimeBy(timeInMillis: Long = DEFAULT_FLUSH_INTERVAL_IN_MILLIS) {
    this.scheduler.advanceTimeBy(timeInMillis)
    this.scheduler.runCurrent()
}
