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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

private const val SOURCE_CONFIG_ENDPOINT = "/sourceConfig"
private const val PLATFORM = "p"
private const val VERSION = "v"
private const val BUILD_VERSION = "bv"
private const val ANDROID = "android"
private const val KOTLIN = "kotlin"
private const val WRITE_KEY = "writeKey"

private const val SOURCE_CONFIG_RETRY_ATTEMPT = 5
private val SOURCE_CONFIG_REFRESH_INTERVAL = 1.hours
private val MIN_TEST_REFRESH_INTERVAL = 1.minutes

private typealias FailureHandler = suspend (NetworkErrorStatus) -> SourceConfig?

/**
 * Fetches the source config at launch and while the app is in the foreground, and notifies its observers.
 */
@InternalRudderApi
class SourceConfigManager(
    private val analytics: Analytics,
    private val sourceConfigState: State<SourceConfig>,
    private val httpClientFactory: HttpClient = analytics.createGetHttpClientFactory(),
) {

    private val logger get() = analytics.logger

    // The refresh loop waits for this job, so the launch fetch and a refresh never run at the same time.
    private var launchFetchJob: Job? = null
    private var refreshJob: Job? = null

    @Volatile
    private var lastFetchEndMillis: Long? = null

    @Volatile
    private var refreshInterval = SOURCE_CONFIG_REFRESH_INTERVAL

    internal fun fetchCachedSourceConfigAndNotifyObservers() {
        readCachedSourceConfig()?.let(::notifyObservers)
    }

    internal fun refreshSourceConfigAndNotifyObservers() {
        launchFetchJob = analytics.notifyOnlyOnceOnConnectionAvailable {
            fetchAndNotifyObservers(onFailure = ::handleLaunchFetchFailure)
        }
    }

    /**
     * Starts the foreground refresh loop, which fetches the source config once per refresh interval.
     */
    @InternalRudderApi
    fun startPeriodicRefresh() {
        refreshJob = refreshJob.createIfInactive(newJob = ::refreshPeriodically)
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
    fun setRefreshIntervalForTesting(interval: Duration) {
        refreshInterval = interval.coerceAtLeast(MIN_TEST_REFRESH_INTERVAL)
        if (refreshJob?.isActive == true) {
            stopPeriodicRefresh()
            startPeriodicRefresh()
        }
    }

    private fun readCachedSourceConfig(): SourceConfig? {
        val cachedSourceConfig = analytics.storage.readString(
            StorageKeys.SOURCE_CONFIG_PAYLOAD,
            defaultVal = String.empty()
        )
        if (cachedSourceConfig.isEmpty()) {
            logger.debug("SourceConfigManager: SourceConfig not found in storage")
            return null
        }
        return LenientJson.decodeFromString<SourceConfig>(cachedSourceConfig).also {
            logger.debug("SourceConfigManager: SourceConfig fetched from storage: $it")
        }
    }

    private fun refreshPeriodically() = analytics.analyticsScope.launch(analytics.analyticsDispatcher) {
        launchFetchJob?.join()
        while (isActive) {
            delay(timeUntilNextFetch(lastFetchEndMillis, refreshInterval))
            analytics.connectivityState.flow.first { it }
            fetchAndNotifyObservers(onFailure = ::handleRefreshFailure)
        }
    }

    private suspend fun fetchAndNotifyObservers(onFailure: FailureHandler) {
        downloadSourceConfig(onFailure)?.let { sourceConfig ->
            storeSourceConfig(sourceConfig)
            notifyObservers(sourceConfig)
        }
        lastFetchEndMillis = DateTimeUtils.getSystemCurrentTime()
    }

    private suspend fun downloadSourceConfig(onFailure: FailureHandler): SourceConfig? {
        return withContext(analytics.networkDispatcher) {
            safelyExecute(logger) { requestSourceConfig(onFailure) }
        }
    }

    private suspend fun requestSourceConfig(onFailure: FailureHandler): SourceConfig? {
        return when (val result = httpClientFactory.getData()) {
            is Result.Success -> result.toSourceConfig(logger)
            is Result.Failure -> onFailure(result.error)
        }
    }

    @OptIn(UseWithCaution::class)
    private suspend fun handleLaunchFetchFailure(error: NetworkErrorStatus): SourceConfig? = when (error) {
        NetworkErrorStatus.Error400 -> {
            logger.error(
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
            logger.debug("SourceConfigManager: ${error.formatStatusCodeMessage()}. Retrying to fetch SourceConfig.")
            retryWithBackoff()
        }
    }

    private suspend fun retryWithBackoff(): SourceConfig? {
        val backOffPolicy = provideBackoffPolicy()
        repeat(SOURCE_CONFIG_RETRY_ATTEMPT) { attempt ->
            delay(backOffPolicy.nextDelayInMillis().milliseconds)
            logger.verbose("SourceConfigManager: Retrying fetching of SourceConfig, attempt: ${attempt + 1}")
            requestSourceConfig { null }?.let { return it }
        }
        logger.warn(
            "SourceConfigManager: All retry attempts for fetching SourceConfig " +
                "have been exhausted. Returning null"
        )
        return null
    }

    // A refresh never shuts down the SDK: uploads and the next launch already handle an invalid write key.
    private fun handleRefreshFailure(error: NetworkErrorStatus): SourceConfig? {
        logger.debug(
            "SourceConfigManager: ${error.formatStatusCodeMessage()}. " +
                "Keeping the current SourceConfig until the next refresh."
        )
        return null
    }

    private suspend fun storeSourceConfig(sourceConfig: SourceConfig) {
        withContext(analytics.keyValueStorageDispatcher) {
            logger.verbose("SourceConfigManager: Storing sourceConfig in storage")
            sourceConfig.storeSourceConfig(analytics.storage)
        }
    }

    private fun notifyObservers(sourceConfig: SourceConfig) {
        logger.verbose("SourceConfigManager: Notifying observers with sourceConfig")
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

private fun timeUntilNextFetch(lastFetchEndMillis: Long?, interval: Duration): Duration {
    lastFetchEndMillis ?: return Duration.ZERO
    val elapsed = (DateTimeUtils.getSystemCurrentTime() - lastFetchEndMillis).milliseconds
    return if (elapsed.isNegative()) Duration.ZERO else (interval - elapsed).coerceAtLeast(Duration.ZERO)
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
