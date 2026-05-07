package com.rudderstack.testapp.analytics

import android.app.Application
import com.rudderstack.scenarioengine.domain.step.Step
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.SessionConfiguration
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.policies.CountFlushPolicy

/**
 * Builds an [Analytics] instance from a [Step.Init].
 *
 * Single place that knows how engine-level Init knobs map to SDK [Configuration] fields.
 * The engine deliberately disables most of the SDK's "convenience-on" defaults (lifecycle
 * tracking, deep links, automatic sessions) because tests want to opt into them
 * explicitly. `mockServerUrl` is wired to *both* `dataPlaneUrl` and `controlPlaneUrl` so a
 * single mock server can serve `/sourceConfig` and `/v1/batch`.
 *
 * `flushAt` overrides the SDK's default flush policies entirely with a single
 * [CountFlushPolicy] — gives scenarios tight control over when the queue drains.
 */
internal object AnalyticsFactory {

    /** Construct a fully-configured [Analytics] from [step]. Returns immediately; the SDK initializes asynchronously. */
    fun create(application: Application, step: Step.Init): Analytics {
        // Read sessionTimeoutMs into a local val so the smart-cast works — Kotlin won't
        // smart-cast a public property from another module (here, :scenarioengine), since
        // a property is contractually a getter that could return different values per call.
        val sessionTimeoutMs = step.sessionTimeoutMs
        val sessionConfiguration = if (sessionTimeoutMs == null) {
            // Use the SDK's own default timeout; only override automaticSessionTracking.
            SessionConfiguration(automaticSessionTracking = step.automaticSessionTracking)
        } else {
            SessionConfiguration(
                automaticSessionTracking = step.automaticSessionTracking,
                sessionTimeoutInMillis = sessionTimeoutMs,
            )
        }

        val configuration = Configuration(
            application = application,
            writeKey = step.writeKey,
            dataPlaneUrl = step.mockServerUrl,
            controlPlaneUrl = step.mockServerUrl,
            trackApplicationLifecycleEvents = step.trackApplicationLifecycleEvents,
            trackDeepLinks = step.trackDeepLinks,
            trackActivities = step.trackActivities,
            sessionConfiguration = sessionConfiguration,
            flushPolicies = listOf(CountFlushPolicy(flushAt = step.flushAt)),
            // The SDK defaults to LogLevel.NONE — useless for a test SUT. Force verbose so
            // every track / identify / lifecycle call produces a logcat line that the driver
            // (and humans debugging) can rely on.
            logLevel = Logger.LogLevel.VERBOSE,
        )

        return Analytics(configuration)
    }
}
