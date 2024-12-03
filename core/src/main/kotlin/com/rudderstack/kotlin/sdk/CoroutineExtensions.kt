package com.rudderstack.kotlin.sdk

import androidx.annotation.RestrictTo
import com.rudderstack.kotlin.sdk.internals.logger.LoggerAnalytics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

// todo: figure out a way to make this a class level property. It will cause issues when multiple instances of sdk will be created.
internal var analyticsJob: Job

@OptIn(ExperimentalCoroutinesApi::class)
private val coroutineConfig: CoroutineConfiguration = object : CoroutineConfiguration {
    private val handler = CoroutineExceptionHandler { _, exception ->
        LoggerAnalytics.error(exception.stackTraceToString())
    }
    override val analyticsScope: CoroutineScope = run {
        analyticsJob = SupervisorJob()
        CoroutineScope(analyticsJob + handler)
    }
    override val analyticsDispatcher: CoroutineDispatcher = Dispatchers.IO
    override val storageDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(2)
    override val networkDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
}

/**
 * The [CoroutineScope] used for running analytics tasks. This scope controls the lifecycle of coroutines within the SDK.
 */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
val analyticsScope: CoroutineScope
    get() = coroutineConfig.analyticsScope

/**
 * The [CoroutineDispatcher] used for executing general analytics tasks in the SDK.
 */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
val analyticsDispatcher: CoroutineDispatcher
    get() = coroutineConfig.analyticsDispatcher

/**
 * The [CoroutineDispatcher] dedicated to executing storage-related tasks, such as reading and writing to disk.
 */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
val storageDispatcher: CoroutineDispatcher
    get() = coroutineConfig.storageDispatcher

/**
 * The [CoroutineDispatcher] dedicated to executing network-related tasks, such as sending events to the data plane.
 */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
val networkDispatcher: CoroutineDispatcher
    get() = coroutineConfig.networkDispatcher
