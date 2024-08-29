package com.rudderstack.core

import com.rudderstack.core.internals.logger.KotlinLogger
import com.rudderstack.core.internals.logger.Logger
import com.rudderstack.core.internals.storage.BasicStorageProvider
import com.rudderstack.core.internals.storage.Storage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

open class Configuration @JvmOverloads constructor(
    open val writeKey: String,
    open val dataPlaneUrl: String,
    open val logger: Logger = KotlinLogger(initialLogLevel = Logger.LogLevel.DEBUG),
    open val optOut: Boolean = false,
    open val gzipEnabled: Boolean = DEFAULT_GZIP_STATUS,
    open val storageProvider: Storage = BasicStorageProvider.getStorage(writeKey, "test application"),
) {
    companion object {
        const val DEFAULT_GZIP_STATUS: Boolean = true
    }
}

interface CoroutineConfiguration {
    val analyticsScope: CoroutineScope
    val analyticsDispatcher: CoroutineDispatcher
    val storageDispatcher: CoroutineDispatcher
    val networkDispatcher: CoroutineDispatcher
}