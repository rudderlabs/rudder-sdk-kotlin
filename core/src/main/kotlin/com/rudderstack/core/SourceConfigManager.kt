package com.rudderstack.core

import com.rudderstack.core.internals.models.SourceConfig
import com.rudderstack.core.internals.network.Failure
import com.rudderstack.core.internals.network.HttpClientImpl
import com.rudderstack.core.internals.network.Success
import com.rudderstack.core.internals.utils.LenientJson
import com.rudderstack.core.internals.utils.encodeToBase64
import kotlinx.coroutines.withContext

private const val SOURCE_CONFIG_ENDPOINT = "sourceConfig"

suspend fun Analytics.fetchSourceConfig() {
    val authHeaderString: String = configuration.writeKey.encodeToBase64()
    val query = this.configuration.storageProvider.getLibraryVersion().toMap()

    withContext(networkDispatcher) {
        val sourceConfig: SourceConfig? = downloadSourceConfig(authHeaderString, query)

        // TODO("Store the sourceConfig in the storage")
    }
}

private fun Analytics.downloadSourceConfig(authHeaderString: String, query: Map<String, String>): SourceConfig? = try {
    val sourceConfigResult = HttpClientImpl.createGetHttpClient(
        baseUrl = configuration.controlPlaneUrl,
        endPoint = SOURCE_CONFIG_ENDPOINT,
        authHeaderString = authHeaderString,
        query = query,
    ).getData()

    when (sourceConfigResult) {
        is Success -> {
            val config = LenientJson.decodeFromString<SourceConfig>(sourceConfigResult.response)
            configuration.logger.info(log = "SourceConfig is fetched successfully: $config")
            config
        }

        is Failure -> {
            configuration.logger.error(log = "Failed to get sourceConfig due to ${sourceConfigResult.status} ${sourceConfigResult.error}")
            null
        }
    }
} catch (e: Exception) {
    configuration.logger.error(log = "Failed to get sourceConfig due to $e")
    null
}
