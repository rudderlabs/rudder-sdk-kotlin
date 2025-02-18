package com.rudderstack.integration.kotlin.facebook

import com.rudderstack.sdk.kotlin.android.Analytics
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.BufferedReader
import java.io.File

fun mockAnalytics(testScope: TestScope, testDispatcher: TestDispatcher): Analytics {
    val mockAnalytics = mockk<Analytics>(relaxed = true)

    mockAnalytics.also {
        every { it.analyticsScope } returns testScope
        every { it.analyticsDispatcher } returns testDispatcher
        every { it.storageDispatcher } returns testDispatcher
        every { it.networkDispatcher } returns testDispatcher
        every { it.integrationsDispatcher } returns testDispatcher
    }

    return mockAnalytics
}

fun Any.readJsonObjectFromFile(filePath: String): JsonObject {
    val inputStream = this::class.java.classLoader?.getResourceAsStream(filePath)
    val a = inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
    return Json.parseToJsonElement(a).jsonObject
}
