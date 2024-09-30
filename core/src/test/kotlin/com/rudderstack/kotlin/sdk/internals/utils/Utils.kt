package com.rudderstack.kotlin.sdk.internals.utils

import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.models.emptyJsonObject
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope

const val ANONYMOUS_ID = "<anonymous-id>"

fun mockAnalytics(testScope: TestScope, testDispatcher: TestDispatcher): Analytics {
    val mock = mockk<Analytics>(relaxed = true)
    every { mock.analyticsScope } returns testScope
    every { mock.analyticsDispatcher } returns testDispatcher
    every { mock.storageDispatcher } returns testDispatcher
    every { mock.networkDispatcher } returns testDispatcher
    return mock
}

fun Message.applyMockedValues() {
    this.originalTimestamp = "<original-timestamp>"
    this.context = emptyJsonObject
    this.messageId = "<message-id>"
}
