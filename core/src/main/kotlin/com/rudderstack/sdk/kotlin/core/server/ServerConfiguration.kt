package com.rudderstack.sdk.kotlin.core.server

import com.rudderstack.sdk.kotlin.core.Configuration
import com.rudderstack.sdk.kotlin.core.StorageType
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.policies.FlushPolicy
import com.rudderstack.sdk.kotlin.core.server.ServerConfiguration.Companion.DEFAULT_MAX_QUEUED_EVENTS
import com.rudderstack.sdk.kotlin.core.server.ServerConfiguration.Companion.DEFAULT_MAX_RETRIES
import com.rudderstack.sdk.kotlin.core.server.ServerConfiguration.Companion.DEFAULT_SERVER_GZIP_STATUS

/**
 * The configuration of the SDK for server-side use (public beta).
 *
 * This configuration activates server mode. In server mode the SDK limits the queue, limits the upload retries,
 * and reports each drop to the [dropListener].
 *
 * @param writeKey The write key of the source.
 * @param dataPlaneUrl The URL of the data plane.
 * @param gzipEnabled `true` to compress each upload with GZIP. Defaults to [DEFAULT_SERVER_GZIP_STATUS].
 * @param flushPolicies The policies that decide when the SDK uploads events. Defaults to [Configuration.DEFAULT_FLUSH_POLICIES].
 * @param storageType The storage type. Defaults to [StorageType.IN_MEMORY]. The queue limit applies to [StorageType.IN_MEMORY] only.
 * @param logger The logger. Defaults to the logger of the SDK.
 * @param logLevel The minimum level of a message that the SDK logs. Defaults to [Configuration.DEFAULT_LOG_LEVEL].
 * @param maxQueuedEvents The maximum number of events that the SDK holds in memory. Defaults to [DEFAULT_MAX_QUEUED_EVENTS].
 * A value below 1 sets the default.
 * @param maxRetries The maximum number of retries for the upload of one batch. Defaults to [DEFAULT_MAX_RETRIES].
 * A value below 0 sets the default.
 * @property dropListener The listener that receives a report for each drop. Defaults to `null`.
 */
class ServerConfiguration @JvmOverloads constructor(
    writeKey: String,
    dataPlaneUrl: String,
    gzipEnabled: Boolean = DEFAULT_SERVER_GZIP_STATUS,
    flushPolicies: List<FlushPolicy> = DEFAULT_FLUSH_POLICIES,
    storageType: StorageType = DEFAULT_STORAGE_TYPE,
    logger: Logger = DEFAULT_LOGGER,
    logLevel: Logger.LogLevel = DEFAULT_LOG_LEVEL,
    maxQueuedEvents: Int = DEFAULT_MAX_QUEUED_EVENTS,
    maxRetries: Int = DEFAULT_MAX_RETRIES,
    val dropListener: DropListener? = null,
) : Configuration(
    writeKey = writeKey,
    dataPlaneUrl = dataPlaneUrl,
    gzipEnabled = gzipEnabled,
    flushPolicies = flushPolicies,
    storageType = storageType,
    logger = logger,
    logLevel = logLevel,
) {

    /**
     * The maximum number of events that the SDK holds in memory. The SDK drops a new event when the queue is full.
     */
    val maxQueuedEvents: Int = maxQueuedEvents.takeIf { it >= 1 } ?: DEFAULT_MAX_QUEUED_EVENTS

    /**
     * The maximum number of retries for the upload of one batch. The SDK drops the batch after the last retry fails.
     */
    val maxRetries: Int = maxRetries.takeIf { it >= 0 } ?: DEFAULT_MAX_RETRIES

    override fun toString(): String {
        return "ServerConfiguration(" +
            "writeKey='$writeKey', " +
            "dataPlaneUrl='$dataPlaneUrl', " +
            "gzipEnabled=$gzipEnabled, " +
            "flushPolicies=$flushPolicies, " +
            "storageType=$storageType, " +
            "logLevel=$logLevel, " +
            "maxQueuedEvents=$maxQueuedEvents, " +
            "maxRetries=$maxRetries" +
            ")"
    }

    companion object {

        /**
         * The default GZIP status of an upload in server mode.
         */
        const val DEFAULT_SERVER_GZIP_STATUS: Boolean = true

        /**
         * The default maximum number of events that the SDK holds in memory.
         */
        const val DEFAULT_MAX_QUEUED_EVENTS: Int = 20_000

        /**
         * The default maximum number of retries for the upload of one batch.
         */
        const val DEFAULT_MAX_RETRIES: Int = 3
    }
}

internal fun Configuration.asServerConfigurationOrNull(): ServerConfiguration? = this as? ServerConfiguration
