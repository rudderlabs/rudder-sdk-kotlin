package com.rudderstack.core

import com.rudderstack.core.internals.logger.KotlinLogger
import com.rudderstack.core.internals.logger.Logger

open class Configuration @JvmOverloads constructor(
    open val writeKey: String,
    open val dataPlaneUrl: String,
    open val logger: Logger = KotlinLogger(initialLogLevel = Logger.DEFAULT_LOG_LEVEL),
    open var optOut: Boolean = false,
)