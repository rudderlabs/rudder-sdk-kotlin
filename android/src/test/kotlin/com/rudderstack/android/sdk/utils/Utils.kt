package com.rudderstack.android.sdk.utils

import android.net.Uri
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import com.rudderstack.android.sdk.Analytics as AndroidAnalytics

fun mockAnalytics(testScope: TestScope, testDispatcher: TestDispatcher): Analytics {
    val mockAnalytics = mockk<AndroidAnalytics>(relaxed = true)

    mockAnalytics.also {
        every { it.analyticsScope } returns testScope
        every { it.analyticsDispatcher } returns testDispatcher
        every { it.storageDispatcher } returns testDispatcher
        every { it.networkDispatcher } returns testDispatcher
    }

    return mockAnalytics
}

// mocks an android URI
fun mockUri(
    scheme: String = "https",
    host: String = "www.test.com",
    path: String = "",
    queryParameters: Map<String, String> = emptyMap(),
    fragment: String = "",
    isHierarchical: Boolean = false,
): Uri {
    val query = queryParameters.entries.joinToString("&") { "${it.key}=${it.value}" }
    val url = StringBuilder().apply {
        if (scheme.isNotEmpty()) {
            append(scheme)
            append("://")
        }
        append(host)
        append(path)
        if (queryParameters.isNotEmpty()) {
            append("?")
            append(query)
        }
        if (fragment.isNotEmpty()) {
            append("#")
            append(fragment)
        }
    }.toString()
    return mockk<Uri>(relaxed = true) {
        every { this@mockk.scheme } returns scheme
        every { this@mockk.host } returns host
        every { this@mockk.path } returns path
        every { this@mockk.query } returns query
        every { this@mockk.fragment } returns fragment
        every { this@mockk.isHierarchical } returns isHierarchical
        every { this@mockk.queryParameterNames } returns queryParameters.keys
        queryParameters.forEach { (key, value) ->
            every { this@mockk.getQueryParameter(key) } returns value
        }
        every { this@mockk.toString() } returns url
    }
}

fun setupLogger(logger: Logger, level: Logger.LogLevel = Logger.LogLevel.VERBOSE) {
    LoggerAnalytics.setup(logger = logger, logLevel = level)
}
