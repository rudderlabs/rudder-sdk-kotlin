package com.rudderstack.core

import com.rudderstack.core.internals.models.SourceConfig
import com.rudderstack.core.internals.network.HttpClient
import com.rudderstack.core.internals.network.HttpClientImpl
import com.rudderstack.core.internals.network.Result
import com.rudderstack.core.internals.statemanagement.SingleThreadStore
import com.rudderstack.core.internals.utils.LenientJson
import com.rudderstack.core.internals.utils.encodeToBase64
import com.rudderstack.core.state.SourceConfigState
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting

private const val SOURCE_CONFIG_ENDPOINT = "/sourceConfig"

internal class ServerConfigManager(
    private val analytics: Analytics,
    private val store: SingleThreadStore<SourceConfigState, SourceConfigState.Update>,
    private val httpClientFactory: HttpClient = analytics.createGetHttpClientFactory(),
) {

    suspend fun fetchSourceConfig() {
        withContext(analytics.networkDispatcher) {
            val sourceConfig: SourceConfig? = downloadSourceConfig()
            sourceConfig?.let { storeSourceConfig(it) }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun downloadSourceConfig(): SourceConfig? {
        return try {
            when (val sourceConfigResult = httpClientFactory.getData()) {
                is Result.Success -> {
                    val config = LenientJson.decodeFromString<SourceConfig>(sourceConfigResult.response)
                    analytics.configuration.logger.info(log = "SourceConfig is fetched successfully: $config")
                    config
                }

                is Result.Failure -> {
                    analytics.configuration.logger.error(
                        log = "Failed to get sourceConfig due to ${sourceConfigResult.status} ${sourceConfigResult.error}"
                    )
                    null
                }
            }
        } catch (e: Exception) {
            analytics.configuration.logger.error(log = "Failed to get sourceConfig due to $e")
            null
        }
    }

    @VisibleForTesting
    fun storeSourceConfig(sourceConfig: SourceConfig) {
        store.subscribe { _, dispatch -> dispatch(SourceConfigState.Update(sourceConfig)) }
        analytics.configuration.logger.debug(log = "SourceConfig: $sourceConfig")
    }
}

internal fun Analytics.createGetHttpClientFactory(): HttpClient {
    val authHeaderString: String = configuration.writeKey.encodeToBase64()
    val query = configuration.storageProvider.getLibraryVersion().toMap()

    return HttpClientImpl.createGetHttpClient(
        baseUrl = configuration.controlPlaneUrl,
        endPoint = SOURCE_CONFIG_ENDPOINT,
        authHeaderString = authHeaderString,
        query = query,
    )
}
