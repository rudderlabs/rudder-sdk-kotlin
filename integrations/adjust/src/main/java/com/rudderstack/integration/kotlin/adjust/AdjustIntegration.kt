package com.rudderstack.integration.kotlin.adjust

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.AdjustEvent
import com.adjust.sdk.AdjustInstance
import com.adjust.sdk.LogLevel
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.serialization.json.JsonObject
import com.rudderstack.integration.kotlin.adjust.AdjustConfig as AdjustDestinationConfig

private const val ANONYMOUS_ID = "anonymousId"

private const val USER_ID = "userId"

/**
 * AdjustIntegration is a plugin that intercepts the track events and logs them.
 */
class AdjustIntegration : IntegrationPlugin() {

    override val key: String
        get() = "Adjust"

    private var adjustInstance: AdjustInstance? = null

    // TODO("We need a way to update this value dynamically.")
    private lateinit var eventToTokenMappings: List<EventToTokenMapping>

    public override fun create(destinationConfig: JsonObject) {
        adjustInstance ?: run {
            destinationConfig.parseConfig<AdjustDestinationConfig>().let { config ->
                eventToTokenMappings = config.eventToTokenMappings
                adjustInstance = initAdjust(
                    application = analytics.application,
                    appToken = config.appToken,
                    logLevel = analytics.configuration.logLevel,
                )
            }
        }
    }

    override fun getDestinationInstance(): Any? {
        return adjustInstance
    }

    override fun identify(payload: IdentifyEvent): Event {
        payload.setSessionParams()

        return payload
    }

    override fun track(payload: TrackEvent): Event {
        eventToTokenMappings.getTokenOrNull(payload.event)?.let { eventToken ->
            payload.setSessionParams()
            val adjustEvent = initAdjustEvent(eventToken).apply {
                addCallbackParameter(payload.properties)
                setRevenue(payload.properties)
                addCallbackParameter(payload.context.toJsonObject(PropertiesConstants.TRAITS))
            }
            Adjust.trackEvent(adjustEvent)
        } ?: run {
            LoggerAnalytics.error(
                "AdjustIntegration: Either Event to Token mapping is not configured in the dashboard " +
                    "or the corresponding token is empty. Therefore dropping the ${payload.event} event."
            )
        }

        return payload
    }

    override fun reset() {
        Adjust.removeGlobalPartnerParameters()
    }

    private fun Event.setSessionParams() {
        Adjust.addGlobalCallbackParameter(ANONYMOUS_ID, anonymousId)
        if (userId.isNotBlank()) {
            Adjust.addGlobalCallbackParameter(USER_ID, userId)
        }
    }

    private fun AdjustEvent.addCallbackParameter(jsonObject: JsonObject) {
        jsonObject.keys.forEach { key ->
            addCallbackParameter(key, jsonObject.getStringOrNull(key))
        }
    }

    private fun AdjustEvent.setRevenue(jsonObject: JsonObject) {
        jsonObject.getDoubleOrNull(PropertiesConstants.REVENUE)?.let { revenue ->
            jsonObject.getStringOrNull(PropertiesConstants.CURRENCY)?.let { currency ->
                setRevenue(revenue, currency)
            }
        }
    }
}

private fun initAdjust(application: Application, appToken: String, logLevel: Logger.LogLevel): AdjustInstance {
    val adjustEnvironment = getAdjustEnvironment(logLevel)
    val adjustConfig = initAdjustConfig(application, appToken, adjustEnvironment)
        .apply {
            setLogLevel(logLevel)
            setAllListeners()
            enableSendingInBackground()
        }
    Adjust.initSdk(adjustConfig)
    application.registerActivityLifecycleCallbacks()
    return Adjust.getDefaultInstance()
}

private fun getAdjustEnvironment(logLevel: Logger.LogLevel): String {
    return if (logLevel >= Logger.LogLevel.DEBUG) {
        AdjustConfig.ENVIRONMENT_SANDBOX
    } else {
        AdjustConfig.ENVIRONMENT_PRODUCTION
    }
}

@VisibleForTesting
internal fun initAdjustConfig(application: Application, appToken: String, adjustEnvironment: String) =
    AdjustConfig(application, appToken, adjustEnvironment)

private fun AdjustConfig.setLogLevel(logLevel: Logger.LogLevel) {
    when (logLevel) {
        Logger.LogLevel.VERBOSE -> setLogLevel(LogLevel.VERBOSE)
        Logger.LogLevel.DEBUG -> setLogLevel(LogLevel.DEBUG)
        Logger.LogLevel.INFO -> setLogLevel(LogLevel.INFO)
        Logger.LogLevel.WARN -> setLogLevel(LogLevel.WARN)
        Logger.LogLevel.ERROR -> setLogLevel(LogLevel.ERROR)
        Logger.LogLevel.NONE -> setLogLevel(LogLevel.SUPPRESS)
    }
}

private fun AdjustConfig.setAllListeners() {
    setOnAttributionChangedListener { attribution ->
        Log.d("AdjustFactory", "Attribution callback called!")
        Log.d("AdjustFactory", "Attribution: $attribution")
    }
    setOnEventTrackingSucceededListener { adjustEventSuccess ->
        Log.d("AdjustFactory", "Event success callback called!")
        Log.d("AdjustFactory", "Event success data: $adjustEventSuccess")
    }
    setOnEventTrackingFailedListener { adjustEventFailure ->
        Log.d("AdjustFactory", "Event failure callback called!")
        Log.d("AdjustFactory", "Event failure data: $adjustEventFailure")
    }
    setOnSessionTrackingSucceededListener { adjustSessionSuccess ->
        Log.d("AdjustFactory", "Session success callback called!")
        Log.d("AdjustFactory", "Session success data: $adjustSessionSuccess")
    }
    setOnSessionTrackingFailedListener { adjustSessionFailure ->
        Log.d("AdjustFactory", "Session failure callback called!")
        Log.d("AdjustFactory", "Session failure data: $adjustSessionFailure")
    }
    setOnDeferredDeeplinkResponseListener { deeplink ->
        Log.d("AdjustFactory", "Deferred deep link callback called!")
        Log.d("AdjustFactory", "Deep link URL: $deeplink")
        true
    }
}

private fun Application.registerActivityLifecycleCallbacks() {
    registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
            // Not supported
        }

        override fun onActivityStarted(activity: Activity) {
            // Not supported
        }

        override fun onActivityResumed(activity: Activity) {
            Adjust.onResume()
        }

        override fun onActivityPaused(activity: Activity) {
            Adjust.onPause()
        }

        override fun onActivityStopped(activity: Activity) {
            // Not supported
        }

        override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
            // Not supported
        }

        override fun onActivityDestroyed(activity: Activity) {
            // Not supported
        }
    })
}

@VisibleForTesting
internal fun initAdjustEvent(eventToken: String) = AdjustEvent(eventToken)
