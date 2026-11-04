package com.rudderstack.sdk.kotlin.core

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
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
import com.rudderstack.sdk.kotlin.core.internals.utils.DateTimeUtils
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution
import com.rudderstack.sdk.kotlin.core.internals.utils.createIfInactive
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToBase64
import com.rudderstack.sdk.kotlin.core.internals.utils.handleInvalidWriteKey
import com.rudderstack.sdk.kotlin.core.internals.utils.notifyOnlyOnceOnConnectionAvailable
import com.rudderstack.sdk.kotlin.core.internals.utils.safelyExecute
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private const val SOURCE_CONFIG_ENDPOINT = "/sourceConfig"
private const val PLATFORM = "p"
private const val VERSION = "v"
private const val BUILD_VERSION = "bv"
private const val ANDROID = "android"
private const val KOTLIN = "kotlin"
private const val WRITE_KEY = "writeKey"

private const val SOURCE_CONFIG_RETRY_ATTEMPT = 5
private val SOURCE_CONFIG_REFRESH_INTERVAL_MILLIS = 1.hours.inWholeMilliseconds
private val MIN_TEST_REFRESH_INTERVAL_MILLIS = 1.minutes.inWholeMilliseconds

/**
 * Manager for handling source config.
 */
@InternalRudderApi
class SourceConfigManager(
    private val analytics: Analytics,
    private val sourceConfigState: State<SourceConfig>,
    private val httpClientFactory: HttpClient = analytics.createGetHttpClientFactory(),
) {

    private var launchFetchJob: Job? = null
    private var refreshJob: Job? = null

    @Volatile
    private var lastFetchEndMillis: Long? = null

    @Volatile
    private var refreshIntervalMillis = SOURCE_CONFIG_REFRESH_INTERVAL_MILLIS

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
                analytics.logger.debug("SourceConfigManager: SourceConfig fetched from storage: $sourceConfig")
                sourceConfig
            }
        } else {
            analytics.logger.debug("SourceConfigManager: SourceConfig not found in storage")
            null
        }
    }

    internal fun refreshSourceConfigAndNotifyObservers() {
        launchFetchJob = this.analytics.notifyOnlyOnceOnConnectionAvailable {
            downloadSourceConfig()?.let { storeAndNotifyObservers(it) }
            markFetchEnded()
        }
    }

    /**
     * Starts the foreground refresh loop, which fetches the source config once per refresh interval.
     */
    @InternalRudderApi
    fun startPeriodicRefresh() {
        refreshJob = refreshJob.createIfInactive {
            analytics.analyticsScope.launch(analytics.analyticsDispatcher) {
                launchFetchJob?.join()
                while (isActive) {
                    delay(timeUntilNextFetchMillis(lastFetchEndMillis, refreshIntervalMillis))
                    analytics.connectivityState.flow.first { it }
                    fetchOnceAndNotifyObservers()
                    markFetchEnded()
                }
            }
        }
    }

    /**
     * Stops the foreground refresh loop and discards the result of a fetch in progress.
     */
    @InternalRudderApi
    fun stopPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    /**
     * Sets a shorter refresh interval for end-to-end tests, with a minimum of one minute. Call it on the main thread.
     */
    @InternalRudderApi
    fun setRefreshIntervalForTesting(intervalMillis: Long) {
        refreshIntervalMillis = intervalMillis.coerceAtLeast(MIN_TEST_REFRESH_INTERVAL_MILLIS)
        if (refreshJob?.isActive == true) {
            stopPeriodicRefresh()
            startPeriodicRefresh()
        }
    }

    private fun markFetchEnded() {
        lastFetchEndMillis = DateTimeUtils.getSystemCurrentTime()
    }

    // A refresh makes one attempt and never shuts down the SDK, so a 400 from a cache miss cannot delete stored events.
    private suspend fun fetchOnceAndNotifyObservers() {
        val sourceConfig = withContext(analytics.networkDispatcher) {
            safelyExecute(analytics.logger) {
                fetchSourceConfigWithFallback { failure ->
                    analytics.logger.debug(
                        "SourceConfigManager: ${failure.error.formatStatusCodeMessage()}. " +
                            "Keeping the current SourceConfig until the next refresh."
                    )
                    null
                }
            }
        }
        sourceConfig?.let { storeAndNotifyObservers(it) }
    }

    private suspend fun storeAndNotifyObservers(sourceConfig: SourceConfig) {
        storeSourceConfig(sourceConfig)
        notifyObservers(sourceConfig)
    }

    private suspend fun downloadSourceConfig(): SourceConfig? {
        return withContext(analytics.networkDispatcher) {
            safelyExecute(analytics.logger) {
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
                analytics.logger.error(
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
            NetworkErrorStatus.ErrorNetworkUnavailable,
            NetworkErrorStatus.ErrorTimeout -> {
                analytics.logger.debug(
                    "SourceConfigManager: ${error.formatStatusCodeMessage()}. Retrying to fetch SourceConfig."
                )
                fetchSourceConfigWithBackOff()
            }
        }

    private suspend fun fetchSourceConfigWithBackOff(): SourceConfig? {
        val backOffPolicy = provideBackoffPolicy()
        repeat(SOURCE_CONFIG_RETRY_ATTEMPT) { attempt ->
            delay(backOffPolicy.nextDelayInMillis())
            analytics.logger.verbose("SourceConfigManager: Retrying fetching of SourceConfig, attempt: ${attempt + 1}")

            fetchSourceConfigWithFallback { null }?.let { return it }
        }
        analytics.logger.warn(
            "SourceConfigManager: All retry attempts for fetching SourceConfig " +
                "have been exhausted. Returning null"
        )
        return null
    }

    private inline fun fetchSourceConfigWithFallback(
        onFailure: (Result.Failure<NetworkErrorStatus>) -> SourceConfig?
    ): SourceConfig? {
        return when (val result = httpClientFactory.getData()) {
            is Result.Success -> result.toSourceConfig(analytics.logger)
            is Result.Failure -> onFailure(result)
        }
    }

    private suspend fun storeSourceConfig(sourceConfig: SourceConfig) {
        withContext(analytics.keyValueStorageDispatcher) {
            analytics.logger.verbose("SourceConfigManager: Storing sourceConfig in storage")
            sourceConfig.storeSourceConfig(analytics.storage)
        }
    }

    private fun notifyObservers(sourceConfig: SourceConfig) {
        analytics.logger.verbose("SourceConfigManager: Notifying observers with sourceConfig")
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
        logger = logger,
    )
}

@VisibleForTesting
internal fun Analytics.getQuery() = when (getPlatformType()) {
    PlatformType.Mobile -> {
        mapOf(
            PLATFORM to ANDROID,
            VERSION to this.storage.getLibraryVersion().getVersionName(),
            WRITE_KEY to this.configuration.writeKey,
            BUILD_VERSION to this.storage.getLibraryVersion().getBuildVersion()
        )
    }

    PlatformType.Server -> {
        mapOf(
            PLATFORM to KOTLIN,
            VERSION to this.storage.getLibraryVersion().getVersionName(),
            WRITE_KEY to this.configuration.writeKey,
        )
    }
}

private fun timeUntilNextFetchMillis(lastFetchEndMillis: Long?, intervalMillis: Long): Long {
    val elapsedMillis = lastFetchEndMillis?.let { DateTimeUtils.getSystemCurrentTime() - it } ?: return 0
    return if (elapsedMillis < 0) 0 else (intervalMillis - elapsedMillis).coerceAtLeast(0)
}

private fun Result.Success<String>.toSourceConfig(logger: Logger): SourceConfig {
    val config = LenientJson.decodeFromString<SourceConfig>(response)
    logger.debug("SourceConfigManager: SourceConfig fetched successfully: $config")
    return config
}

@VisibleForTesting
internal fun provideBackoffPolicy(): BackOffPolicy {
    return ExponentialBackOffPolicy()
}
