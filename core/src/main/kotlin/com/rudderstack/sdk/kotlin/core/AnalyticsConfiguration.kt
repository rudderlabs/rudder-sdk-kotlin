package com.rudderstack.sdk.kotlin.core

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

/**
 * Internal configuration for the analytics module.
 */
@InternalRudderApi
interface AnalyticsConfiguration {

    /**
     * Storage implementation for the analytics module.
     */
    val storage: Storage

    /**
     * Scope for analytics coroutines.
     */
    val analyticsScope: CoroutineScope

    /**
     * Dispatcher for analytics coroutines.
     */
    val analyticsDispatcher: CoroutineDispatcher

    /**
     * Dispatcher for storage related tasks.
     */
    val storageDispatcher: CoroutineDispatcher

    /**
     * Dispatcher for network related tasks.
     */
    val networkDispatcher: CoroutineDispatcher

    /**
     * Job for analytics coroutines.
     */
    val analyticsJob: Job
}

@OptIn(ExperimentalCoroutinesApi::class)
private class AnalyticsConfigurationImpl(
    override val storage: Storage
) : AnalyticsConfiguration {

    private val handler = CoroutineExceptionHandler { _, exception ->
        LoggerAnalytics.error(exception.stackTraceToString())
    }
    override val analyticsJob: Job = SupervisorJob()
    override val analyticsScope: CoroutineScope = run {
        CoroutineScope(analyticsJob + handler)
    }
    override val analyticsDispatcher: CoroutineDispatcher = Dispatchers.IO
    override val storageDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(2)
    override val networkDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
}

/**
 * Get the analytics configuration.
 */
fun provideAnalyticsConfiguration(storage: Storage): AnalyticsConfiguration {
    return AnalyticsConfigurationImpl(storage)
}
