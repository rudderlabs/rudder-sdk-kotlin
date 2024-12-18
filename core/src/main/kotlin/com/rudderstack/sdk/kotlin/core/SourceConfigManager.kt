package com.rudderstack.sdk.kotlin.core

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClient
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClientImpl
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.FlowState
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToBase64
import kotlinx.coroutines.withContext

private const val SOURCE_CONFIG_ENDPOINT = "/sourceConfig"
private const val PLATFORM = "p"
private const val VERSION = "v"
private const val BUILD_VERSION = "bv"
private const val ANDROID = "android"
private const val KOTLIN = "kotlin"

internal class SourceConfigManager(
    private val analytics: Analytics,
    private val sourceConfigState: FlowState<SourceConfig>,
    private val httpClientFactory: HttpClient = analytics.createGetHttpClientFactory(),
) {

    suspend fun fetchAndUpdateSourceConfig() {
        val downloadedSourceConfig = downloadSourceConfig()
        downloadedSourceConfig?.let {
            updateSourceConfigState(it)
            storeSourceConfig(it)
        } ?: run {
            val fetchedSourceConfig = fetchStoredSourceConfig()
            fetchedSourceConfig?.let { updateSourceConfigState(it) }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun downloadSourceConfig(): SourceConfig? {
        return withContext(analytics.networkDispatcher) {
            try {
                when (val sourceConfigResult = httpClientFactory.getData()) {
                    is Result.Success -> {
                        val config = LenientJson.decodeFromString<SourceConfig>(sourceConfigResult.response)
                        LoggerAnalytics.info("SourceConfig is fetched successfully: $config")
                        config
                    }

                    is Result.Failure -> {
                        LoggerAnalytics.error(
                            "Failed to get sourceConfig due to ${sourceConfigResult.status} ${sourceConfigResult.error}"
                        )
                        null
                    }
                }
            } catch (e: Exception) {
                LoggerAnalytics.error("Failed to get sourceConfig due to $e")
                null
            }
        }
    }

    private fun fetchStoredSourceConfig(): SourceConfig? {
        val sourceConfigString = analytics.storage.readString(
            StorageKeys.SOURCE_CONFIG_PAYLOAD,
            defaultVal = String.empty()
        )

        return if (sourceConfigString.isNotEmpty()) {
            val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigString)
            LoggerAnalytics.info("SourceConfig fetched from storage: $sourceConfig")
            sourceConfig
        } else {
            LoggerAnalytics.info("SourceConfig not found in storage")
            null
        }
    }

    private suspend fun storeSourceConfig(sourceConfig: SourceConfig) {
        withContext(analytics.storageDispatcher) {
            sourceConfig.storeSourceConfig(analytics.storage)
        }
    }

    private fun updateSourceConfigState(sourceConfig: SourceConfig) {
        sourceConfigState.dispatch(SourceConfig.UpdateAction(sourceConfig))
    }
}

private fun Analytics.createGetHttpClientFactory(): HttpClient {
    val authHeaderString: String = configuration.writeKey.encodeToBase64()
    val query = getQuery()

    return HttpClientImpl.createGetHttpClient(
        baseUrl = configuration.controlPlaneUrl,
        endPoint = SOURCE_CONFIG_ENDPOINT,
        authHeaderString = authHeaderString,
        query = query,
    )
}

private fun Analytics.getQuery() = when (getPlatformType()) {
    PlatformType.Mobile -> {
        mapOf(
            PLATFORM to ANDROID,
            VERSION to this.storage.getLibraryVersion().getVersionName(),
            BUILD_VERSION to this.storage.getLibraryVersion().getBuildVersion()
        )
    }

    PlatformType.Server -> {
        mapOf(
            PLATFORM to KOTLIN,
            VERSION to this.storage.getLibraryVersion().getVersionName(),
        )
    }
}
