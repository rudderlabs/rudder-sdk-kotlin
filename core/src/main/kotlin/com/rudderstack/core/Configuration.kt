package com.rudderstack.core

import com.rudderstack.core.internals.logger.KotlinLogger
import com.rudderstack.core.internals.logger.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

open class Configuration @JvmOverloads constructor(
    open val writeKey: String,
    open val dataPlaneUrl: String,
    open val logger: Logger = KotlinLogger(initialLogLevel = Logger.LogLevel.DEBUG),
    open val optOut: Boolean = false,
    open val gzipEnabled: Boolean = DEFAULT_GZIP_STATUS,
) {
    companion object {
        const val DEFAULT_GZIP_STATUS: Boolean = true
    }
}

interface CoroutineConfiguration {
    val analyticsScope: CoroutineScope
    val analyticsDispatcher: CoroutineDispatcher
    val networkDispatcher: CoroutineDispatcher
}