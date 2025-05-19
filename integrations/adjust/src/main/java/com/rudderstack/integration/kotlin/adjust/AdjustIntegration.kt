package com.rudderstack.integration.kotlin.adjust

import android.app.Activity
import android.app.Application
import androidx.annotation.VisibleForTesting
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.AdjustEvent
import com.adjust.sdk.AdjustInstance
import com.adjust.sdk.LogLevel
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.StandardIntegration
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.addLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import kotlinx.serialization.json.JsonObject
import com.rudderstack.integration.kotlin.adjust.AdjustConfig as AdjustDestinationConfig

private const val ANONYMOUS_ID = "anonymousId"

private const val USER_ID = "userId"
private const val ADJUST_KEY = "Adjust"

/**
 * AdjustIntegration is a plugin that sends events to the Adjust SDK.
 */
@OptIn(InternalRudderApi::class)
class AdjustIntegration : StandardIntegration, IntegrationPlugin(), ActivityLifecycleObserver {

    override val key: String
        get() = ADJUST_KEY

    private var adjustInstance: AdjustInstance? = null

    private lateinit var eventToTokenMappings: List<EventToTokenMapping>

    public override fun create(destinationConfig: JsonObject) {
        adjustInstance ?: run {
            destinationConfig.parseConfig<AdjustDestinationConfig>()?.let { config ->
                eventToTokenMappings = config.eventToTokenMappings
                adjustInstance = initAdjust(
                    application = analytics.application,
                    appToken = config.appToken,
                    logLevel = LoggerAnalytics.logLevel,
                )
                (analytics as? Analytics)?.addLifecycleObserver(this)
                LoggerAnalytics.verbose("AdjustIntegration: Adjust SDK initialized.")
            }
        }
    }

    override fun getDestinationInstance(): Any? {
        return adjustInstance
    }

    override fun update(destinationConfig: JsonObject) {
        destinationConfig.parseConfig<AdjustDestinationConfig>()?.let { updatedConfig ->
            this.eventToTokenMappings = updatedConfig.eventToTokenMappings
        }
    }

    override fun identify(payload: IdentifyEvent) {
        payload.setSessionParams()
    }

    override fun track(payload: TrackEvent) {
        eventToTokenMappings.getTokenOrNull(payload.event)?.let { eventToken ->
            payload.setSessionParams()
            val adjustEvent = initAdjustEvent(eventToken).apply {
                addCallbackParameter(payload.properties)
                setRevenue(payload.properties)
                addCallbackParameter(payload.context.toJsonObject(PropertiesConstants.TRAITS))
            }
            Adjust.trackEvent(adjustEvent)
            LoggerAnalytics.verbose("AdjustIntegration: Track event sent to Adjust.")
        } ?: run {
            LoggerAnalytics.error(
                "AdjustIntegration: Either Event to Token mapping is not configured in the dashboard " +
                    "or the corresponding token is empty. Therefore dropping the ${payload.event} event."
            )
        }
    }

    override fun reset() {
        Adjust.removeGlobalPartnerParameters()
        LoggerAnalytics.verbose("AdjustIntegration: Reset call completed.")
    }

    private fun Event.setSessionParams() {
        Adjust.addGlobalPartnerParameter(ANONYMOUS_ID, anonymousId)
        if (userId.isNotBlank()) {
            Adjust.addGlobalPartnerParameter(USER_ID, userId)
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

    override fun onActivityResumed(activity: Activity) {
        Adjust.onResume()
    }

    override fun onActivityPaused(activity: Activity) {
        Adjust.onPause()
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
    return Adjust.getDefaultInstance()
}

private fun getAdjustEnvironment(logLevel: Logger.LogLevel): String {
    return if (logLevel != Logger.LogLevel.NONE) {
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
        LoggerAnalytics.debug("Adjust: Attribution callback called!")
        LoggerAnalytics.debug("Adjust: Attribution: $attribution")
    }
    setOnEventTrackingSucceededListener { adjustEventSuccess ->
        LoggerAnalytics.debug("Adjust: Event success callback called!")
        LoggerAnalytics.debug("Adjust: Event success data: $adjustEventSuccess")
    }
    setOnEventTrackingFailedListener { adjustEventFailure ->
        LoggerAnalytics.debug("Adjust: Event failure callback called!")
        LoggerAnalytics.debug("Adjust: Event failure data: $adjustEventFailure")
    }
    setOnSessionTrackingSucceededListener { adjustSessionSuccess ->
        LoggerAnalytics.debug("Adjust: Session success callback called!")
        LoggerAnalytics.debug("Adjust: Session success data: $adjustSessionSuccess")
    }
    setOnSessionTrackingFailedListener { adjustSessionFailure ->
        LoggerAnalytics.debug("Adjust: Session failure callback called!")
        LoggerAnalytics.debug("Adjust: Session failure data: $adjustSessionFailure")
    }
    setOnDeferredDeeplinkResponseListener { deeplink ->
        LoggerAnalytics.debug("Adjust: Deferred deep link callback called!")
        LoggerAnalytics.debug("Adjust: Deep link URL: $deeplink")
        true
    }
}

@VisibleForTesting
internal fun initAdjustEvent(eventToken: String) = AdjustEvent(eventToken)
