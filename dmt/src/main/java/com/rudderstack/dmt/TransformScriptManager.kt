package com.rudderstack.dmt

import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClient
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClientImpl
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.StateAction
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToBase64
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.util.zip.GZIPInputStream

// todo: remove this base url field
private const val BASE_URL = "https://f0d1c3gv-3000.inc1.devtunnels.ms/"
private const val DMT_ENDPOINT = "/file"
private const val PLATFORM = "p"
private const val VERSION = "v"
private const val BUILD_VERSION = "bv"
private const val ANDROID = "android"
private const val KOTLIN = "kotlin"

internal class TransformScriptManager(
    private val analytics: Analytics,
    private val quickJSWrapper: QuickJSWrapper,
    private val httpClientFactory: HttpClient = analytics.createGetHttpClientFactory(),
) {

    private val jsScriptState: State<String> = State(initialState = fetchCachedJSScript())

    private lateinit var convertorScript: String

    internal fun updateJSScriptAndNotify() {
        analytics.analyticsScope.launch(analytics.networkDispatcher) {
            downloadJSScript()?.let { script ->
                val transformedScript = transformScript(script)
                storeJSScript(transformedScript)
                notifyObservers(transformedScript)
            }
        }
    }

    internal fun transformEvent(event: Event): Event? {
        if (jsScriptState.value.isEmpty()) {
            LoggerAnalytics.debug("TransformScriptManager: No JS transformation script present.")
            return event
        }

        val originalJson = LenientJson.encodeToString(event)
        LoggerAnalytics.debug("TransformScriptManager: Original JSON - $originalJson")

        val transformedJson = quickJSWrapper.processEvent(originalJson, script = jsScriptState.value)
        LoggerAnalytics.debug("TransformScriptManager: Transformed JSON - $transformedJson")

        if (transformedJson.isEmpty()) return null

        val transformedEvent = LenientJson.decodeFromString<Event>(transformedJson)
        LoggerAnalytics.debug("TransformScriptManager: Successfully transformed event '${event}' to '${transformedEvent}'")

        return transformedEvent
    }

    private fun fetchCachedJSScript(): String {
        return analytics.storage.readString(key = StorageKeys.DMT_SCRIPT, defaultVal = String.empty())
    }

    private fun downloadJSScript(): String? {
        return when (val result = httpClientFactory.getData()) {
            is Result.Success -> result.response
            is Result.Failure -> null
        }
    }

    private fun notifyObservers(updatedScript: String) {
        jsScriptState.dispatch(JSScriptUpdateAction(updatedScript))
    }

    private suspend fun storeJSScript(script: String) {
        analytics.storage.write(StorageKeys.DMT_SCRIPT, script)
    }

    private suspend fun transformScript(script: String): String {
        return withContext(analytics.analyticsDispatcher) {
            if (!::convertorScript.isInitialized) {
                convertorScript = loadCompressedJS()
            }
            quickJSWrapper.convertScript(script, convertorScript)
        }
    }

    private fun loadCompressedJS(): String {
        val inputStream = GZIPInputStream(analytics.application.assets.open("babel.js.gz"))
        return inputStream.bufferedReader().use { it.readText() }
    }
}

private fun Analytics.createGetHttpClientFactory(): HttpClient {
    val authHeaderString: String = configuration.writeKey.encodeToBase64()
    val query = getQuery()

    return HttpClientImpl.createGetHttpClient(
        baseUrl = BASE_URL,
        endPoint = DMT_ENDPOINT,
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

private class JSScriptUpdateAction(
    private val updatedScript: String
) : StateAction<String> {

    override fun reduce(currentState: String): String {
        return updatedScript
    }
}
