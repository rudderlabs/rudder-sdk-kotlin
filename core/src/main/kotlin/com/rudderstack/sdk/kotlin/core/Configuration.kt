package com.rudderstack.sdk.kotlin.core

import com.rudderstack.sdk.kotlin.core.Configuration.Companion.DEFAULT_CONTROL_PLANE_URL
import com.rudderstack.sdk.kotlin.core.Configuration.Companion.DEFAULT_FLUSH_POLICIES
import com.rudderstack.sdk.kotlin.core.Configuration.Companion.DEFAULT_GZIP_STATUS
import com.rudderstack.sdk.kotlin.core.internals.policies.CountFlushPolicy
import com.rudderstack.sdk.kotlin.core.internals.policies.FlushPolicy
import com.rudderstack.sdk.kotlin.core.internals.policies.FrequencyFlushPolicy
import com.rudderstack.sdk.kotlin.core.internals.policies.StartupFlushPolicy
import org.jetbrains.annotations.VisibleForTesting

/**
 * The `Configuration` class is used to configure the SDK's settings for network communication, logging, data storage, and more.
 * This class provides customizable options such as API keys, URLs for data and control planes, logging levels, and storage preferences.
 *
 * @property writeKey The write key provided by RudderStack to authenticate API requests.
 * @property dataPlaneUrl The URL of the data plane where all event data will be sent.
 * @property controlPlaneUrl The URL of the control plane for fetching configuration settings. Defaults to [DEFAULT_CONTROL_PLANE_URL].
 * @property gzipEnabled A flag indicating whether GZIP compression is enabled for network requests. Defaults to [DEFAULT_GZIP_STATUS].
 * @property flushPolicies A list of flush policies that determine when to flush events to the data plane. Defaults to [DEFAULT_FLUSH_POLICIES].
 */
open class Configuration @JvmOverloads constructor(
    open val writeKey: String,
    open val dataPlaneUrl: String,
    open val controlPlaneUrl: String = DEFAULT_CONTROL_PLANE_URL,
    open val gzipEnabled: Boolean = DEFAULT_GZIP_STATUS,
    open val flushPolicies: List<FlushPolicy> = DEFAULT_FLUSH_POLICIES,
) {

    companion object {

        /**
         * The default status of GZIP compression for network requests.
         * If true, GZIP is enabled; if false, it is disabled.
         */
        const val DEFAULT_GZIP_STATUS: Boolean = false

        /**
         * The default URL for the control plane, used for fetching configuration settings.
         */
        const val DEFAULT_CONTROL_PLANE_URL = "https://api.rudderlabs.com"

        /**
         * The default flush policies for `CountFlushPolicy` and `FrequencyFlushPolicy`,
         * which define the conditions under which events are flushed to the server.
         */
        val DEFAULT_FLUSH_POLICIES: List<FlushPolicy>
            get() = provideListOfFlushPolicies()
    }
}

/**
 * Provides a list of default flush policies used for sending events to the data plane.
 *
 * @return A list of flush policies, including `CountFlushPolicy`, `FrequencyFlushPolicy`, and `StartupFlushPolicy`.
 */
@VisibleForTesting
fun provideListOfFlushPolicies() = listOf(
    CountFlushPolicy(),
    FrequencyFlushPolicy(),
    StartupFlushPolicy(),
)
