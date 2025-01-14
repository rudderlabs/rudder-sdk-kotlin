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
import com.rudderstack.sdk.kotlin.core.internals.utils.notifyOnlyOnceOnConnectionAvailable
import com.rudderstack.sdk.kotlin.core.internals.utils.safelyExecute
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

    internal fun fetchCachedSourceConfigAndNotifyObservers() {
        fetchCachedSourceConfig()?.let { sourceConfig ->
            notifyObservers(sourceConfig)
        }
    }

    private fun fetchCachedSourceConfig(): SourceConfig? {
        val cachedSourceConfig = analytics.storage.readString(
            StorageKeys.SOURCE_CONFIG_PAYLOAD,
            defaultVal = String.empty()
        )

        return if (cachedSourceConfig.isNotEmpty()) {
            LenientJson.decodeFromString<SourceConfig>(cachedSourceConfig).let { sourceConfig ->
                LoggerAnalytics.info("SourceConfig fetched from storage: $sourceConfig")
                sourceConfig
            }
        } else {
            LoggerAnalytics.info("SourceConfig not found in storage")
            null
        }
    }

    internal fun refreshSourceConfigAndNotifyObservers() {
        this.analytics.notifyOnlyOnceOnConnectionAvailable {
            downloadSourceConfig()?.let { sourceConfig ->
                storeSourceConfig(sourceConfig)
                notifyObservers(sourceConfig)
            }
        }
    }

    private suspend fun downloadSourceConfig(): SourceConfig? {
        return withContext(analytics.networkDispatcher) {
            safelyExecute {
                when (val sourceConfigResult = httpClientFactory.getData()) {
                    is Result.Success -> {
                        LenientJson.decodeFromString<SourceConfig>(sourceConfigResult.response).let { config ->
                            LoggerAnalytics.info("SourceConfig is fetched successfully: $config")
                            config
                        }
                    }

                    is Result.Failure -> {
                        LoggerAnalytics.error(
                            "Failed to get sourceConfig due to ${sourceConfigResult.status} ${sourceConfigResult.error}"
                        )
                        null
                    }
                }
            }
        }
    }

    private suspend fun storeSourceConfig(sourceConfig: SourceConfig) {
        withContext(analytics.storageDispatcher) {
            LoggerAnalytics.verbose("Storing sourceConfig in storage.")
            sourceConfig.storeSourceConfig(analytics.storage)
        }
    }

    private fun notifyObservers(sourceConfig: SourceConfig) {
        LoggerAnalytics.verbose("Notifying observers with sourceConfig.")
        sourceConfigState.dispatch(SourceConfig.NotifyObserversAction(sourceConfig))
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
