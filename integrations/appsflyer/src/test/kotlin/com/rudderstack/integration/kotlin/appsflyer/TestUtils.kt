package com.rudderstack.integration.kotlin.appsflyer

import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.serialization.json.JsonObject

fun mockAnalytics(testScope: TestScope, testDispatcher: TestDispatcher): Analytics {
    val mockAnalytics = mockk<Analytics>(relaxed = true)

    mockAnalytics.also {
        every { it.analyticsScope } returns testScope
        every { it.analyticsDispatcher } returns testDispatcher
        every { it.fileStorageDispatcher } returns testDispatcher
        every { it.networkDispatcher } returns testDispatcher
        every { it.integrationsDispatcher } returns testDispatcher
        every { it.sourceConfigState } returns State(initialState = SourceConfig.initialState())
    }

    return mockAnalytics
}

infix fun JsonObject.mergeWithHigherPriorityTo(other: JsonObject): JsonObject {
    return JsonObject(this.toMap() + other.toMap())
}
