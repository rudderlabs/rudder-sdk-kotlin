package com.rudderstack.android.utils

import com.rudderstack.core.Analytics
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope

fun mockAnalytics(testScope: TestScope, testDispatcher: TestDispatcher): Analytics {
    val mockAnalytics = mockk<Analytics>(relaxed = true)

    mockAnalytics.also {
        every { it.analyticsScope } returns testScope
        every { it.analyticsDispatcher } returns testDispatcher
        every { it.storageDispatcher } returns testDispatcher
        every { it.networkDispatcher } returns testDispatcher
    }

    return mockAnalytics
}
