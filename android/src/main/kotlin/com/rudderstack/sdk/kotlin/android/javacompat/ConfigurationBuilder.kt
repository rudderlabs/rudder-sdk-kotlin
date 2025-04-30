package com.rudderstack.sdk.kotlin.android.javacompat

import android.app.Application
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.Configuration.Companion.DEFAULT_COLLECT_DEVICE_ID
import com.rudderstack.sdk.kotlin.android.Configuration.Companion.DEFAULT_TRACK_ACTIVITIES
import com.rudderstack.sdk.kotlin.android.Configuration.Companion.DEFAULT_TRACK_APPLICATION_LIFECYCLE_EVENTS
import com.rudderstack.sdk.kotlin.android.Configuration.Companion.DEFAULT_TRACK_DEEP_LINKS
import com.rudderstack.sdk.kotlin.android.DEFAULT_SESSION_TIMEOUT_IN_MILLIS
import com.rudderstack.sdk.kotlin.android.SessionConfiguration
import com.rudderstack.sdk.kotlin.android.SessionConfiguration.Companion.DEFAULT_AUTOMATIC_SESSION_TRACKING
import com.rudderstack.sdk.kotlin.core.Configuration.Companion.DEFAULT_CONTROL_PLANE_URL
import com.rudderstack.sdk.kotlin.core.Configuration.Companion.DEFAULT_FLUSH_POLICIES
import com.rudderstack.sdk.kotlin.core.Configuration.Companion.DEFAULT_GZIP_STATUS
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.policies.FlushPolicy

/**
 * Builder class for creating Configuration instances with a fluent API.
 *
 * This builder provides a convenient way to create Configuration objects step by step,
 * especially useful in Java code where Kotlin's default arguments aren't available.
 */
class ConfigurationBuilder(
    private val application: Application,
    private val writeKey: String,
    private val dataPlaneUrl: String,
) {

    private var trackApplicationLifecycleEvents: Boolean = DEFAULT_TRACK_APPLICATION_LIFECYCLE_EVENTS
    private var trackDeepLinks: Boolean = DEFAULT_TRACK_DEEP_LINKS
    private var trackActivities: Boolean = DEFAULT_TRACK_ACTIVITIES
    private var collectDeviceId: Boolean = DEFAULT_COLLECT_DEVICE_ID
    private var sessionConfiguration: SessionConfiguration = SessionConfigurationBuilder().build()
    private var controlPlaneUrl: String = DEFAULT_CONTROL_PLANE_URL
    private var logLevel: Logger.LogLevel = Logger.DEFAULT_LOG_LEVEL
    private var flushPolicies: List<FlushPolicy> = DEFAULT_FLUSH_POLICIES
    private var gzipEnabled: Boolean = DEFAULT_GZIP_STATUS

    /**
     * Sets whether to track application lifecycle events.
     */
    fun withTrackApplicationLifecycleEvents(track: Boolean) = apply {
        trackApplicationLifecycleEvents = track
    }

    /**
     * Sets whether to track deep links.
     */
    fun withTrackDeepLinks(track: Boolean) = apply {
        trackDeepLinks = track
    }

    /**
     * Sets whether to track activities automatically.
     */
    fun withTrackActivities(track: Boolean) = apply {
        trackActivities = track
    }

    /**
     * Sets whether to collect device ID.
     */
    fun withCollectDeviceId(collect: Boolean) = apply {
        collectDeviceId = collect
    }

    /**
     * Sets the session configuration.
     */
    fun withSessionConfiguration(config: SessionConfiguration) = apply {
        sessionConfiguration = config
    }

    /**
     * Sets the control plane URL.
     */
    fun withControlPlaneUrl(url: String) = apply {
        controlPlaneUrl = url
    }

    /**
     * Sets the log level.
     */
    fun withLogLevel(level: Logger.LogLevel) = apply {
        logLevel = level
    }

    /**
     * Sets the flush policies.
     */
    fun withFlushPolicies(policies: List<FlushPolicy>) = apply {
        flushPolicies = policies
    }

    /**
     * Sets whether to enable GZIP compression.
     */
    fun withGzipEnabled(enabled: Boolean) = apply {
        gzipEnabled = enabled
    }

    /**
     * Builds the Configuration instance with the configured properties.
     */
    fun build(): Configuration {
        return Configuration(
            application = application,
            trackApplicationLifecycleEvents = trackApplicationLifecycleEvents,
            trackDeepLinks = trackDeepLinks,
            trackActivities = trackActivities,
            collectDeviceId = collectDeviceId,
            sessionConfiguration = sessionConfiguration,
            writeKey = writeKey,
            dataPlaneUrl = dataPlaneUrl,
            controlPlaneUrl = controlPlaneUrl,
            logLevel = logLevel,
            flushPolicies = flushPolicies,
            gzipEnabled = gzipEnabled
        )
    }
}

/**
 * Builder for SessionConfiguration to enable easy construction from Java code.
 */
class SessionConfigurationBuilder {

    private var automaticSessionTracking: Boolean = DEFAULT_AUTOMATIC_SESSION_TRACKING
    private var sessionTimeoutInMillis: Long = DEFAULT_SESSION_TIMEOUT_IN_MILLIS

    /**
     * Sets whether to enable automatic session tracking.
     */
    fun withAutomaticSessionTracking(enabled: Boolean) = apply {
        automaticSessionTracking = enabled
    }

    /**
     * Sets the session timeout duration in milliseconds.
     */
    fun withSessionTimeoutInMillis(timeoutInMillis: Long) = apply {
        sessionTimeoutInMillis = timeoutInMillis
    }

    /**
     * Builds the SessionConfiguration with the configured properties.
     */
    fun build(): SessionConfiguration {
        return SessionConfiguration(
            automaticSessionTracking = automaticSessionTracking,
            sessionTimeoutInMillis = sessionTimeoutInMillis
        )
    }
}
