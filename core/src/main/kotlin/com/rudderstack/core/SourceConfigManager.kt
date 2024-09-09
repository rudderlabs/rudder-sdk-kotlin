package com.rudderstack.core

import com.rudderstack.core.internals.models.SourceConfig
import com.rudderstack.core.internals.network.Failure
import com.rudderstack.core.internals.network.HttpClient
import com.rudderstack.core.internals.network.HttpClientImpl
import com.rudderstack.core.internals.network.Success
import com.rudderstack.core.internals.utils.LenientJson
import com.rudderstack.core.internals.utils.encodeToBase64
import kotlinx.coroutines.withContext

private const val SOURCE_CONFIG_ENDPOINT = "/sourceConfig"

class ServerConfigManager(
    private val analytics: Analytics,
    private val httpClientFactory: HttpClient = analytics.createGetHttpClientFactory(),
) {

    suspend fun fetchSourceConfig() {
        withContext(analytics.networkDispatcher) {
            val sourceConfig: SourceConfig? = downloadSourceConfig()

            sourceConfig?.let {
                // Store the sourceConfig in the storage
                storeSourceConfig(it)
            }
        }
    }

    private fun downloadSourceConfig(): SourceConfig? {
        return try {
            when (val sourceConfigResult = httpClientFactory.getData()) {
                is Success -> {
                    val config = LenientJson.decodeFromString<SourceConfig>(sourceConfigResult.response)
                    analytics.configuration.logger.info(log = "SourceConfig is fetched successfully: $config")
                    config
                }

                is Failure -> {
                    analytics.configuration.logger.error(log = "Failed to get sourceConfig due to ${sourceConfigResult.status} ${sourceConfigResult.error}")
                    null
                }
            }
        } catch (e: Exception) {
            analytics.configuration.logger.error(log = "Failed to get sourceConfig due to $e")
            null
        }
    }

    private fun storeSourceConfig(sourceConfig: SourceConfig) {
        // TODO("Store the sourceConfig in the storage")
    }
}

private fun Analytics.createGetHttpClientFactory(): HttpClient {
    val authHeaderString: String = configuration.writeKey.encodeToBase64()
    val query = configuration.storageProvider.getLibraryVersion().toMap()

    return HttpClientImpl.createGetHttpClient(
        baseUrl = configuration.controlPlaneUrl,
        endPoint = SOURCE_CONFIG_ENDPOINT,
        authHeaderString = authHeaderString,
        query = query,
    )
}
