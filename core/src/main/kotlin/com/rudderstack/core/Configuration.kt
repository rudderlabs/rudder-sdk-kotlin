package com.rudderstack.core

import com.rudderstack.core.internals.logger.KotlinLogger
import com.rudderstack.core.internals.logger.Logger
import com.rudderstack.core.internals.policies.CountFlushPolicy
import com.rudderstack.core.internals.policies.FlushPolicy
import com.rudderstack.core.internals.policies.FrequencyFlushPolicy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

open class Configuration @JvmOverloads constructor(
    open val writeKey: String,
    open val dataPlaneUrl: String,
    open val logger: Logger = KotlinLogger(initialLogLevel = Logger.LogLevel.DEBUG),
    open val optOut: Boolean = false,
    open val gzipEnabled: Boolean = DEFAULT_GZIP_STATUS,
    open var flushPolicies: List<FlushPolicy> = DEFAULT_FLUSH_POLICIES,
) {
    companion object {
        const val DEFAULT_GZIP_STATUS: Boolean = true
        val DEFAULT_FLUSH_POLICIES: List<FlushPolicy> = listOf(CountFlushPolicy(), FrequencyFlushPolicy())
    }
}

interface CoroutineConfiguration {
    val analyticsScope: CoroutineScope
    val analyticsDispatcher: CoroutineDispatcher
    val storageDispatcher: CoroutineDispatcher
    val networkDispatcher: CoroutineDispatcher
}
