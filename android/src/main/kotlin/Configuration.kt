package com.rudderstack.android

import android.app.Application
import com.rudderstack.android.storage.AndroidStorageProvider
import com.rudderstack.core.Configuration
import com.rudderstack.core.internals.logger.Logger
import com.rudderstack.core.internals.policies.FlushPolicy

data class Configuration @JvmOverloads constructor(
    val application: Application,
    override val writeKey: String,
    override val dataPlaneUrl: String,
    override val controlPlaneUrl: String = DEFAULT_CONTROL_PLANE_URL,
    override val logger: Logger = AndroidLogger(initialLogLevel = Logger.LogLevel.DEBUG),
    override var flushPolicies: List<FlushPolicy> = DEFAULT_FLUSH_POLICIES,
) : Configuration(
    writeKey = writeKey,
    dataPlaneUrl = dataPlaneUrl,
    storageProvider = AndroidStorageProvider.getStorage(writeKey, application),
    logger = logger,
    flushPolicies = flushPolicies,
)
