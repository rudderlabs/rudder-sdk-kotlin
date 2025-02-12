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
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.serialization.json.JsonObject
import com.rudderstack.integration.kotlin.adjust.AdjustConfig as AdjustDestinationConfig

/**
 * AdjustIntegration is a plugin that intercepts the track events and logs them.
 */
class AdjustIntegration : IntegrationPlugin() {

    override val key: String
        get() = "Adjust"

    private var adjustInstance: AdjustInstance? = null

    // TODO("We need a way to update this value dynamically.")
    private lateinit var eventToTokenMappings: List<EventToTokenMapping>

    override fun create(destinationConfig: JsonObject) {
        adjustInstance ?: run {
            destinationConfig.parseConfig<AdjustDestinationConfig>().let { config ->
                eventToTokenMappings = config.eventToTokenMappings
                adjustInstance = initialiseAdjust(
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

    override fun track(payload: TrackEvent): Event {
        // check pre-defined event map and find out the token for event
        eventToTokenMappings.getTokenOrNull(payload.event)?.let { eventToken ->
            setSessionParams(payload)
            AdjustEvent(eventToken).let { adjustEvent ->
                adjustEvent.addCallbackParameter(payload.properties)
                adjustEvent.setRevenue(payload.properties)
                adjustEvent.addCallbackParameter(payload.context.toJsonObject(Constants.TRAITS))
                Adjust.trackEvent(adjustEvent)
            }
        } ?: run {
            LoggerAnalytics.debug("AdjustIntegration: Event not found in custom mappings.")
        }

        return payload
    }

    private fun setSessionParams(payload: TrackEvent) {
        with(payload) {
            Adjust.addGlobalCallbackParameter("anonymousId", anonymousId)
            userId.takeUnless { it.isBlank() }?.let { userId ->
                Adjust.addGlobalCallbackParameter("userId", userId)
            }
        }
    }

    private fun AdjustEvent.addCallbackParameter(jsonObject: JsonObject) {
        jsonObject.forEach { (key, value) ->
            addCallbackParameter(key, value.toString())
        }
    }

    private fun AdjustEvent.setRevenue(jsonObject: JsonObject) {
        jsonObject.getDoubleOrNull(Constants.REVENUE)?.let { revenue ->
            jsonObject.getStringOrNull(Constants.CURRENCY)?.let { currency ->
                setRevenue(revenue, currency)
            }
        }
    }
}

@VisibleForTesting
internal fun initialiseAdjust(application: Application, appToken: String, logLevel: Logger.LogLevel): AdjustInstance {
    val adjustEnvironment = getAdjustEnvironment(logLevel)
    val adjustConfig = AdjustConfig(application, appToken, adjustEnvironment)
        .also {
            it.setLogLevel(logLevel)
            it.setAllListeners()
            it.enableSendingInBackground()
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
    setOnAttributionChangedListener({ attribution ->
        Log.d("AdjustFactory", "Attribution callback called!")
        Log.d("AdjustFactory", "Attribution: $attribution")
    })
    setOnEventTrackingSucceededListener({ adjustEventSuccess ->
        Log.d("AdjustFactory", "Event success callback called!")
        Log.d("AdjustFactory", "Event success data: $adjustEventSuccess")
    })
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
