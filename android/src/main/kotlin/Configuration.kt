package com.rudderstack.android

import android.app.Application
import com.rudderstack.android.storage.AndroidStorageProvider
import com.rudderstack.core.Configuration
import com.rudderstack.core.internals.logger.Logger

data class Configuration @JvmOverloads constructor(
    val application: Application,
    override val writeKey: String,
    override val dataPlaneUrl: String,
    override val logger: Logger = AndroidLogger(initialLogLevel = Logger.LogLevel.DEBUG),
) : Configuration(
    writeKey = writeKey,
    dataPlaneUrl = dataPlaneUrl,
    storageProvider = AndroidStorageProvider.getStorage(writeKey, application),
    logger = logger
)