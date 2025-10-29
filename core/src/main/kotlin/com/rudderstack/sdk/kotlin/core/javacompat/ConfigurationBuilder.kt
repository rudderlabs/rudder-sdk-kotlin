package com.rudderstack.sdk.kotlin.core.javacompat

import com.rudderstack.sdk.kotlin.core.Configuration
import com.rudderstack.sdk.kotlin.core.Configuration.Companion.DEFAULT_CONTROL_PLANE_URL
import com.rudderstack.sdk.kotlin.core.Configuration.Companion.DEFAULT_FLUSH_POLICIES
import com.rudderstack.sdk.kotlin.core.Configuration.Companion.DEFAULT_GZIP_STATUS
import com.rudderstack.sdk.kotlin.core.Configuration.Companion.DEFAULT_RESET_OPTIONS
import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetOptions
import com.rudderstack.sdk.kotlin.core.internals.policies.FlushPolicy

/**
 * Builder class for creating Configuration instances.
 *
 * This builder class provides Java interop support for configuring the SDK's settings.
 */
open class ConfigurationBuilder(
    private val writeKey: String,
    private val dataPlaneUrl: String,
) {

    private var controlPlaneUrl: String = DEFAULT_CONTROL_PLANE_URL
    private var gzipEnabled: Boolean = DEFAULT_GZIP_STATUS
    private var flushPolicies: List<FlushPolicy> = DEFAULT_FLUSH_POLICIES
    private var defaultResetOptions: ResetOptions = DEFAULT_RESET_OPTIONS

    /**
     * Sets the control plane URL.
     */
    open fun setControlPlaneUrl(url: String) = apply {
        controlPlaneUrl = url
    }

    /**
     * Sets the flush policies.
     */
    open fun setFlushPolicies(policies: List<FlushPolicy>) = apply {
        flushPolicies = policies
    }

    /**
     * Sets whether to enable GZIP compression.
     */
    open fun setGzipEnabled(enabled: Boolean) = apply {
        gzipEnabled = enabled
    }

    /**
     * Sets the default reset options for reset API calls.
     */
    open fun setDefaultResetOptions(options: ResetOptions) = apply {
        defaultResetOptions = options
    }

    /**
     * Builds the Configuration instance with the configured properties.
     */
    open fun build(): Configuration {
        return Configuration(
            writeKey = writeKey,
            dataPlaneUrl = dataPlaneUrl,
            controlPlaneUrl = controlPlaneUrl,
            flushPolicies = flushPolicies,
            gzipEnabled = gzipEnabled
        )
    }
}
