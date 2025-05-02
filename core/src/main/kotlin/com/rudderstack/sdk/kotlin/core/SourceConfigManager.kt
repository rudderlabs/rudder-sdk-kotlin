package com.rudderstack.sdk.kotlin.core

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClient
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClientImpl
import com.rudderstack.sdk.kotlin.core.internals.network.NetworkErrorStatus
import com.rudderstack.sdk.kotlin.core.internals.network.formatStatusCodeMessage
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.policies.backoff.BackOffPolicy
import com.rudderstack.sdk.kotlin.core.internals.policies.backoff.ExponentialBackOffPolicy
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToBase64
import com.rudderstack.sdk.kotlin.core.internals.utils.handleInvalidWriteKey
import com.rudderstack.sdk.kotlin.core.internals.utils.notifyOnlyOnceOnConnectionAvailable
import com.rudderstack.sdk.kotlin.core.internals.utils.safelyExecute
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting

private const val SOURCE_CONFIG_ENDPOINT = "/sourceConfig"
private const val PLATFORM = "p"
private const val VERSION = "v"
private const val BUILD_VERSION = "bv"
private const val ANDROID = "android"
private const val KOTLIN = "kotlin"

private const val SOURCE_CONFIG_RETRY_ATTEMPT = 5

/**
 * Manager for handling source config.
 */
@InternalRudderApi
class SourceConfigManager(
    private val analytics: Analytics,
    private val sourceConfigState: State<SourceConfig>,
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
                fetchSourceConfigWithFallback { failureResult ->
                    getSourceConfigOnFailure(failureResult)
                }
            }
        }
    }

    @OptIn(UseWithCaution::class)
    private suspend fun getSourceConfigOnFailure(result: Result.Failure<NetworkErrorStatus>): SourceConfig? =
        when (val error = result.error) {
            NetworkErrorStatus.Error400 -> {
                LoggerAnalytics.error(
                    "SourceConfigManager: ${error.formatStatusCodeMessage()}. " +
                        "Invalid write key. Ensure the write key is valid."
                )
                analytics.handleInvalidWriteKey()
                null
            }

            NetworkErrorStatus.Error401,
            NetworkErrorStatus.Error404,
            NetworkErrorStatus.Error413,
            is NetworkErrorStatus.ErrorRetry,
            NetworkErrorStatus.ErrorUnknown,
            NetworkErrorStatus.ErrorNetworkUnavailable -> {
                LoggerAnalytics.debug(
                    "SourceConfigManager: ${error.formatStatusCodeMessage()}. Retrying to fetch SourceConfig."
                )
                fetchSourceConfigWithBackOff()
            }
        }

    private suspend fun fetchSourceConfigWithBackOff(): SourceConfig? {
        val backOffPolicy = provideBackoffPolicy()
        repeat(SOURCE_CONFIG_RETRY_ATTEMPT) { attempt ->
            delay(backOffPolicy.nextDelayInMillis())
            LoggerAnalytics.verbose("Retrying fetching of SourceConfig, attempt: ${attempt + 1}")

            fetchSourceConfigWithFallback { null }?.let { return it }
        }
        LoggerAnalytics.info("All retry attempts for fetching SourceConfig have been exhausted. Returning null.")
        return null
    }

    private inline fun fetchSourceConfigWithFallback(
        onFailure: (Result.Failure<NetworkErrorStatus>) -> SourceConfig?
    ): SourceConfig? {
        return when (val result = httpClientFactory.getData()) {
            is Result.Success -> result.toSourceConfig()
            is Result.Failure -> onFailure(result)
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

private fun Result.Success<String>.toSourceConfig(): SourceConfig {
    val config = LenientJson.decodeFromString<SourceConfig>(response)
    LoggerAnalytics.info("SourceConfig is fetched successfully: $config")
    return config
}

@VisibleForTesting
internal fun provideBackoffPolicy(): BackOffPolicy {
    return ExponentialBackOffPolicy()
}
