package com.rudderstack.android

import android.app.Application
import com.rudderstack.android.storage.AndroidStorageProvider
import com.rudderstack.core.Configuration
import com.rudderstack.core.internals.logger.Logger
import com.rudderstack.core.internals.storage.Storage

data class Configuration @JvmOverloads constructor(
    override val writeKey: String,
    val application: Application,
    override val dataPlaneUrl: String,
    override val storageProvider: Storage = AndroidStorageProvider.getStorage(writeKey, application),
    val logLevel: Logger.LogLevel = Logger.DEFAULT_LOG_LEVEL,
    val trackLifecycleEvents: Boolean = TRACK_LIFECYCLE_EVENTS,
    val recordScreenViews: Boolean = RECORD_SCREEN_VIEWS,
    val isPeriodicFlushEnabled: Boolean = IS_PERIODIC_FLUSH_ENABLED,
    val autoCollectAdvertId: Boolean = AUTO_COLLECT_ADVERT_ID,
    val defaultProcessName: String? = DEFAULT_PROCESS_NAME,
    val advertisingId: String? = null,
    val deviceToken: String? = null,
    val collectDeviceId: Boolean = COLLECT_DEVICE_ID,
    val trackAutoSession: Boolean = AUTO_SESSION_TRACKING,
    val sessionTimeoutMillis: Long = SESSION_TIMEOUT,
    override val flushQueueSize: Int = DEFAULT_FLUSH_QUEUE_SIZE,
    override val maxFlushInterval: Long = DEFAULT_MAX_FLUSH_INTERVAL,
    override val shouldVerifySdk: Boolean = SHOULD_VERIFY_SDK,
    override val gzipEnabled: Boolean = GZIP_ENABLED,
    override val controlPlaneUrl: String = DEFAULT_ANDROID_CONTROLPLANE_URL,
) : Configuration(
    writeKey = writeKey,
    storageProvider = storageProvider,
    flushQueueSize = flushQueueSize,
    maxFlushInterval = maxFlushInterval,
    shouldVerifySdk = shouldVerifySdk,
    gzipEnabled = gzipEnabled,
    dataPlaneUrl = dataPlaneUrl,
    controlPlaneUrl = controlPlaneUrl,
    logger = AndroidLogger(logLevel)
) {

    companion object {

        const val COLLECT_DEVICE_ID: Boolean = true
        const val DEFAULT_ANDROID_CONTROLPLANE_URL = "https://api.rudderlabs.com"
        const val GZIP_ENABLED: Boolean = true
        const val SHOULD_VERIFY_SDK: Boolean = true
        const val TRACK_LIFECYCLE_EVENTS = true
        const val RECORD_SCREEN_VIEWS = false
        const val IS_PERIODIC_FLUSH_ENABLED = false
        const val AUTO_COLLECT_ADVERT_ID = false

        @JvmField
        var DEFAULT_PROCESS_NAME: String? = null
        const val DEFAULT_FLUSH_QUEUE_SIZE = 30
        const val DEFAULT_MAX_FLUSH_INTERVAL = 10 * 1000L
        const val SESSION_TIMEOUT: Long = 300000
        const val AUTO_SESSION_TRACKING = true
    }
}
