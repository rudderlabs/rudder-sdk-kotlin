package com.rudderstack.core

import com.rudderstack.core.internals.logger.KotlinLogger
import com.rudderstack.core.internals.logger.Logger
import com.rudderstack.core.internals.models.Message
import com.rudderstack.core.internals.storage.BasicStorageProvider
import com.rudderstack.core.internals.storage.Storage

typealias EventCallBack = (Message, status: Int, message: String) -> Unit

open class Configuration @JvmOverloads constructor(
    open val writeKey: String,
    open val flushQueueSize: Int = FLUSH_QUEUE_SIZE,
    open val maxFlushInterval: Long = MAX_FLUSH_INTERVAL,
    open var optOut: Boolean = false,
    open val shouldVerifySdk: Boolean = false,
    open val gzipEnabled: Boolean = false,
    open val dataPlaneUrl: String = DATA_PLANE_URL,
    open val controlPlaneUrl: String = CONTROL_PLANE_URL,
    open val logger: Logger = KotlinLogger(initialLogLevel = Logger.DEFAULT_LOG_LEVEL),
    open val storageProvider: Storage = BasicStorageProvider.getStorage(writeKey, "test application"),
    open var offline: Boolean? = false,
    open var callback: EventCallBack? = null
) {

    companion object {

        const val FLUSH_QUEUE_SIZE = 30
        const val MAX_FLUSH_INTERVAL = 10 * 1000L //10 seconds
        const val DATA_PLANE_URL = "https://hosted.rudderlabs.com"
        const val CONTROL_PLANE_URL = "https://api.rudderstack.com/"
    }
}
