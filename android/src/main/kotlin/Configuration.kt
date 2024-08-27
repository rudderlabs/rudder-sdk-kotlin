package com.rudderstack.android

import android.app.Application
import com.rudderstack.core.Configuration
import com.rudderstack.core.internals.logger.Logger
import com.rudderstack.core.internals.policies.FlushPolicy

data class Configuration @JvmOverloads constructor(
    override val writeKey: String,
    val application: Application,
    override val dataPlaneUrl: String,
    val logLevel: Logger.LogLevel = Logger.LogLevel.DEBUG,
    override var flushPolicies: List<FlushPolicy> = DEFAULT_FLUSH_POLICIES,
) : Configuration(
    writeKey = writeKey,
    dataPlaneUrl = dataPlaneUrl,
    logger = AndroidLogger(logLevel),
    flushPolicies = flushPolicies,
)
