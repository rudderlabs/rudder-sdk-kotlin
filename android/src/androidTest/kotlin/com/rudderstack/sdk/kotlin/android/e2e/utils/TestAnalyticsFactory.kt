package com.rudderstack.sdk.kotlin.android.e2e.utils

import android.app.Application
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.SessionConfiguration
import com.rudderstack.sdk.kotlin.core.internals.policies.CountFlushPolicy

/**
 * Creates a real [Analytics] instance wired to a MockWebServer for E2E testing.
 *
 * Key configuration choices:
 * - Both `dataPlaneUrl` and `controlPlaneUrl` point to MockWebServer
 * - `CountFlushPolicy(flushAt = 1)` ensures events flush immediately
 * - Lifecycle, deeplink, and session tracking are disabled to prevent auto-generated events
 */
object TestAnalyticsFactory {

    fun create(
        application: Application,
        mockServerUrl: String,
        writeKey: String = "test-write-key",
    ): Analytics {
        val configuration = Configuration(
            application = application,
            writeKey = writeKey,
            dataPlaneUrl = mockServerUrl,
            controlPlaneUrl = mockServerUrl,
            flushPolicies = listOf(CountFlushPolicy(flushAt = 1)),
            trackApplicationLifecycleEvents = false,
            trackDeepLinks = false,
            trackActivities = false,
            collectDeviceId = true,
            sessionConfiguration = SessionConfiguration(
                automaticSessionTracking = false,
            ),
        )
        return Analytics(configuration)
    }
}
