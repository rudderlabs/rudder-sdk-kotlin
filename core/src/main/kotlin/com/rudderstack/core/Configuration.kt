package com.rudderstack.core

import com.rudderstack.core.Configuration.Companion.DEFAULT_CONTROL_PLANE_URL
import com.rudderstack.core.Configuration.Companion.DEFAULT_FLUSH_POLICIES
import com.rudderstack.core.Configuration.Companion.DEFAULT_GZIP_STATUS
import com.rudderstack.core.internals.logger.KotlinLogger
import com.rudderstack.core.internals.logger.Logger
import com.rudderstack.core.internals.policies.CountFlushPolicy
import com.rudderstack.core.internals.policies.FlushPolicy
import com.rudderstack.core.internals.policies.FrequencyFlushPolicy
import com.rudderstack.core.internals.storage.BasicStorageProvider
import com.rudderstack.core.internals.storage.Storage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/**
 * The `Configuration` class is used to configure the SDK's settings for network communication, logging, data storage, and more.
 * This class provides customizable options such as API keys, URLs for data and control planes, logging levels, and storage preferences.
 *
 * @property writeKey The write key provided by RudderStack to authenticate API requests.
 * @property dataPlaneUrl The URL of the data plane where all event data will be sent.
 * @property controlPlaneUrl The URL of the control plane for fetching configuration settings. Defaults to [DEFAULT_CONTROL_PLANE_URL].
 * @property logger The logger instance used for logging events, errors, and debug information. Defaults to a logger with the DEBUG log level.
 * @property optOut A flag indicating whether to opt out of data collection. If set to true, no data will be sent. Defaults to false.
 * @property gzipEnabled A flag indicating whether GZIP compression is enabled for network requests. Defaults to [DEFAULT_GZIP_STATUS].
 * @property storageProvider The storage provider responsible for managing the data storage. Defaults to [BasicStorageProvider].
 * @property flushPolicies A list of flush policies that determine when to flush events to the data plane. Defaults to [DEFAULT_FLUSH_POLICIES].
 */
open class Configuration @JvmOverloads constructor(
    open val writeKey: String,
    open val dataPlaneUrl: String,
    open val controlPlaneUrl: String = DEFAULT_CONTROL_PLANE_URL,
    open val logger: Logger = KotlinLogger(initialLogLevel = Logger.LogLevel.DEBUG),
    open val optOut: Boolean = false,
    open val gzipEnabled: Boolean = DEFAULT_GZIP_STATUS,
    open val storageProvider: Storage = BasicStorageProvider.getStorage(writeKey, "test application"),
    open var flushPolicies: List<FlushPolicy> = DEFAULT_FLUSH_POLICIES,
) {

    companion object {
        /**
         * The default status of GZIP compression for network requests.
         * If true, GZIP is enabled; if false, it is disabled.
         */
        const val DEFAULT_GZIP_STATUS: Boolean = true

        /**
         * The default URL for the control plane, used for fetching configuration settings.
         */
        const val DEFAULT_CONTROL_PLANE_URL = "https://api.rudderlabs.com"

        /**
         * The default flush policies for `CountFlushPolicy` and `FrequencyFlushPolicy`,
         * which define the conditions under which events are flushed to the server.
         */
        val DEFAULT_FLUSH_POLICIES: List<FlushPolicy> = listOf(CountFlushPolicy(), FrequencyFlushPolicy())
    }
}

/**
 * Interface to provide coroutine configuration for the RudderStack SDK.
 * Implementing this interface allows configuring the dispatchers and coroutine scope used in the SDK.
 */
interface CoroutineConfiguration {
    /**
     * The [CoroutineScope] used for running analytics tasks. This scope controls the lifecycle of coroutines within the SDK.
     */
    val analyticsScope: CoroutineScope

    /**
     * The [CoroutineDispatcher] used for executing general analytics tasks in the SDK.
     */
    val analyticsDispatcher: CoroutineDispatcher

    /**
     * The [CoroutineDispatcher] dedicated to executing storage-related tasks, such as reading and writing to disk.
     */
    val storageDispatcher: CoroutineDispatcher

    /**
     * The [CoroutineDispatcher] dedicated to executing network-related tasks, such as sending events to the data plane.
     */
    val networkDispatcher: CoroutineDispatcher
}
