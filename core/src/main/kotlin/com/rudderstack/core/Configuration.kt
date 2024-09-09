package com.rudderstack.core

import com.rudderstack.core.internals.logger.KotlinLogger
import com.rudderstack.core.internals.logger.Logger
import com.rudderstack.core.internals.storage.BasicStorageProvider
import com.rudderstack.core.internals.storage.Storage
import com.rudderstack.core.internals.policies.CountFlushPolicy
import com.rudderstack.core.internals.policies.FlushPolicy
import com.rudderstack.core.internals.policies.FrequencyFlushPolicy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

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
        const val DEFAULT_GZIP_STATUS: Boolean = true
        const val DEFAULT_CONTROL_PLANE_URL = "https://api.rudderlabs.com"
        val DEFAULT_FLUSH_POLICIES: List<FlushPolicy> = listOf(CountFlushPolicy(), FrequencyFlushPolicy())
    }
}

interface CoroutineConfiguration {
    val analyticsScope: CoroutineScope
    val analyticsDispatcher: CoroutineDispatcher
    val storageDispatcher: CoroutineDispatcher
    val networkDispatcher: CoroutineDispatcher
}
