package com.rudderstack.integration.kotlin.sprig

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.StandardIntegration
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.addLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.userleap.EventName
import com.userleap.EventPayload
import com.userleap.Sprig
import com.userleap.SprigLoggingLevel
import kotlinx.serialization.json.JsonObject
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics

private const val SPRIG_KEY = "Sprig"
internal const val EMAIL_KEY = "email"
internal const val MAX_ATTRIBUTE_KEY_LENGTH = 256

/**
 * SprigIntegration is a plugin that sends events to the Sprig SDK.
 */
@OptIn(InternalRudderApi::class)
class SprigIntegration : StandardIntegration, IntegrationPlugin(), ActivityLifecycleObserver {

    override val key: String
        get() = SPRIG_KEY

    private var sprig: Sprig? = null

    @Volatile
    private var currentActivity: FragmentActivity? = null

    public override fun create(destinationConfig: JsonObject) {
        sprig ?: run {
            destinationConfig.parseConfig<SprigConfig>(analytics.logger)?.let { config ->
                setLogLevel(analytics.logger, analytics.configuration.logLevel)
                sprig = Sprig
                sprig?.configure(analytics.application.applicationContext, config.environmentId)
                (analytics as? AndroidAnalytics)?.addLifecycleObserver(this)
                analytics.logger.info("SprigIntegration: Sprig SDK initialized")
            }
        }
    }

    override fun getDestinationInstance(): Any? {
        return sprig
    }

    override fun identify(payload: IdentifyEvent) {
        val sprig = sprig ?: return

        if (payload.userId.isNotBlank()) {
            sprig.setUserIdentifier(payload.userId)
        }

        payload.traits?.let { traits ->
            setSprigAttributes(sprig, traits, analytics.logger)
        }

        analytics.logger.verbose("SprigIntegration: Identify event processed (messageId=${payload.messageId})")
    }

    override fun track(payload: TrackEvent) {
        val sprig = sprig ?: return

        val eventPayload = EventPayload(
            event = payload.event,
            properties = payload.properties.toStringMap(),
        )

        val activity = currentActivity
        if (activity != null) {
            sprig.trackAndPresent(eventPayload, activity)
        } else {
            sprig.track(eventPayload)
        }

        analytics.logger.verbose("SprigIntegration: Track event '${payload.event}' sent (messageId=${payload.messageId})")
    }

    override fun reset() {
        sprig?.logout()
        analytics.logger.debug("SprigIntegration: Reset completed")
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity is FragmentActivity) {
            currentActivity = activity
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity == currentActivity) {
            currentActivity = null
        }
    }
}

/**
 * Supplies the logger that the Sprig SDK lacks: Sprig does not ship its own logger, it only
 * emits log output through `LOGGING_EVENT` callbacks carrying a [SprigLoggingLevel], leaving
 * it to the host to decide where to write them. This function installs that listener and
 * forwards each event to the Rudder [Logger] at the mapped severity, so Sprig output honours
 * the Rudder SDK's [Logger.LogLevel] filter (which the logger applies internally).
 *
 * Skips registration entirely when the Rudder log level is [Logger.LogLevel.NONE].
 */
private fun setLogLevel(logger: Logger, rudderLogLevel: Logger.LogLevel) {
    if (rudderLogLevel == Logger.LogLevel.NONE) return

    Sprig.addEventListener(EventName.LOGGING_EVENT) { event ->
        val message = "SprigIntegration: ${event.logMessage}"
        when (event.logLevel) {
            SprigLoggingLevel.DEBUG -> logger.debug(message)
            SprigLoggingLevel.INFO -> logger.info(message)
            SprigLoggingLevel.WARNING -> logger.warn(message)
            SprigLoggingLevel.ERROR, SprigLoggingLevel.CRITICAL -> logger.error(message)
        }
    }
}
