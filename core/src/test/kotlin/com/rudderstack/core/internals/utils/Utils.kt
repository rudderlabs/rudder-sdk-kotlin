package com.rudderstack.core.internals.utils

import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.models.Message
import com.rudderstack.core.internals.models.emptyJsonObject
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope

fun mockAnalytics(testScope: TestScope, testDispatcher: TestDispatcher): Analytics {
    val mock = mockk<Analytics>(relaxed = true)
    every { mock.analyticsScope } returns testScope
    every { mock.analyticsDispatcher } returns testDispatcher
    every { mock.storageDispatcher } returns testDispatcher
    every { mock.networkDispatcher } returns testDispatcher
    return mock
}

fun Message.applyTestBaseDate() {
    this.originalTimestamp = "<original-timestamp>"
    this.context = emptyJsonObject
    this.messageId = "<message-id>"
}
