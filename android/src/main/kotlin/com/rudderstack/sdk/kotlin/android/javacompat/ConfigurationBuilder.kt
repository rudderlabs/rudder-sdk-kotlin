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
import com.rudderstack.sdk.kotlin.core.javacompat.ConfigurationBuilder

/**
 * Builder class for creating Configuration instances.
 *
 * This builder class provides Java interop support for configuring the SDK's settings.
 */
class ConfigurationBuilder(
    private val application: Application,
    private val writeKey: String,
    private val dataPlaneUrl: String,
) : ConfigurationBuilder(
    writeKey = writeKey,
    dataPlaneUrl = dataPlaneUrl,
) {

    private var trackApplicationLifecycleEvents: Boolean = DEFAULT_TRACK_APPLICATION_LIFECYCLE_EVENTS
    private var trackDeepLinks: Boolean = DEFAULT_TRACK_DEEP_LINKS
    private var trackActivities: Boolean = DEFAULT_TRACK_ACTIVITIES
    private var collectDeviceId: Boolean = DEFAULT_COLLECT_DEVICE_ID
    private var sessionConfiguration: SessionConfiguration = SessionConfigurationBuilder().build()

    /**
     * Sets whether to track application lifecycle events.
     */
    fun setTrackApplicationLifecycleEvents(track: Boolean) = apply {
        trackApplicationLifecycleEvents = track
    }

    /**
     * Sets whether to track deep links.
     */
    fun setTrackDeepLinks(track: Boolean) = apply {
        trackDeepLinks = track
    }

    /**
     * Sets whether to track activities automatically.
     */
    fun setTrackActivities(track: Boolean) = apply {
        trackActivities = track
    }

    /**
     * Sets whether to collect device ID.
     */
    fun setCollectDeviceId(collect: Boolean) = apply {
        collectDeviceId = collect
    }

    /**
     * Sets the session configuration.
     */
    fun setSessionConfiguration(config: SessionConfiguration) = apply {
        sessionConfiguration = config
    }

    /**
     * Builds the Configuration instance with the configured properties.
     */
    override fun build(): Configuration {
        val coreConfig = super.build()

        return Configuration(
            application = application,
            trackApplicationLifecycleEvents = trackApplicationLifecycleEvents,
            trackDeepLinks = trackDeepLinks,
            trackActivities = trackActivities,
            collectDeviceId = collectDeviceId,
            sessionConfiguration = sessionConfiguration,
            writeKey = writeKey,
            dataPlaneUrl = dataPlaneUrl,
            controlPlaneUrl = coreConfig.controlPlaneUrl,
            logLevel = coreConfig.logLevel,
            flushPolicies = coreConfig.flushPolicies,
            gzipEnabled = coreConfig.gzipEnabled,
        )
    }
}

/**
 * Builder for SessionConfiguration instances.
 */
class SessionConfigurationBuilder {

    private var automaticSessionTracking: Boolean = DEFAULT_AUTOMATIC_SESSION_TRACKING
    private var sessionTimeoutInMillis: Long = DEFAULT_SESSION_TIMEOUT_IN_MILLIS

    /**
     * Sets whether to enable automatic session tracking.
     */
    fun setAutomaticSessionTracking(enabled: Boolean) = apply {
        automaticSessionTracking = enabled
    }

    /**
     * Sets the session timeout duration in milliseconds.
     */
    fun setSessionTimeoutInMillis(timeoutInMillis: Long) = apply {
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
