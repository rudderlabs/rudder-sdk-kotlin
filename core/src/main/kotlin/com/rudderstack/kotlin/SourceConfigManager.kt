package com.rudderstack.kotlin

import com.rudderstack.kotlin.internals.models.SourceConfig
import com.rudderstack.kotlin.internals.network.HttpClient
import com.rudderstack.kotlin.internals.network.HttpClientImpl
import com.rudderstack.kotlin.internals.network.Result
import com.rudderstack.kotlin.internals.platform.PlatformType
import com.rudderstack.kotlin.internals.statemanagement.Store
import com.rudderstack.kotlin.internals.utils.LenientJson
import com.rudderstack.kotlin.internals.utils.encodeToBase64
import com.rudderstack.kotlin.state.SourceConfigState
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting

private const val SOURCE_CONFIG_ENDPOINT = "/sourceConfig"
private const val PLATFORM = "p"
private const val VERSION = "v"
private const val BUILD_VERSION = "bv"
private const val ANDROID = "android"
private const val KOTLIN = "kotlin"

internal class SourceConfigManager(
    private val analytics: Analytics,
    private val store: Store<SourceConfigState, SourceConfigState.UpdateAction>,
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
        store.subscribe { _, _ -> analytics.configuration.logger.debug(log = "SourceConfigState subscribed") }
        store.dispatch(action = SourceConfigState.UpdateAction(sourceConfig))
        analytics.configuration.logger.debug(log = "SourceConfig: $sourceConfig")
    }
}

internal fun Analytics.createGetHttpClientFactory(): HttpClient {
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
            VERSION to this.configuration.storage.getLibraryVersion().getVersionName(),
            BUILD_VERSION to this.configuration.storage.getLibraryVersion().getBuildVersion()
        )
    }

    PlatformType.Server -> {
        mapOf(
            PLATFORM to KOTLIN,
            VERSION to this.configuration.storage.getLibraryVersion().getVersionName(),
        )
    }
}
