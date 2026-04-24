package com.rudderstack.integration.kotlin.sprig

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.StandardIntegration
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.addLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.android.utils.removeLifecycleObserver
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.userleap.EventListener
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

    private var loggingListener: EventListener? = null

    @Volatile
    private var currentActivity: FragmentActivity? = null

    public override fun create(destinationConfig: JsonObject) {
        sprig ?: run {
            destinationConfig.parseConfig<SprigConfig>(analytics.logger)?.let { config ->
                loggingListener = registerSprigLogging(analytics.logger, analytics.configuration.logLevel)
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
        if (activity == null) {
            sprig.track(eventPayload)
        } else {
            activity.runOnUiThread {
                if (activity.canHostSurvey()) {
                    sprig.trackAndPresent(eventPayload, activity)
                } else {
                    sprig.track(eventPayload)
                }
            }
        }

        analytics.logger.verbose("SprigIntegration: Track event '${payload.event}' sent (messageId=${payload.messageId})")
    }

    private fun FragmentActivity.canHostSurvey(): Boolean =
        !isFinishing && !isDestroyed && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    override fun reset() {
        sprig?.logout()
        analytics.logger.debug("SprigIntegration: Reset completed")
    }

    /**
     * Sets the current [FragmentActivity] that the Sprig SDK will use to present in-app
     * experiences (such as surveys) when a track event triggers one.
     *
     * Recommended contract:
     * - Call `setFragmentActivity(activity)` from `onResume`.
     * - Call `setFragmentActivity(null)` from `onPause`.
     *
     * Clearing in `onPause` prevents the Sprig SDK from attempting to commit a fragment
     * transaction on an activity that has already saved its instance state (which would
     * throw `IllegalStateException`). The integration also clears the reference in
     * [onActivityDestroyed] as a safety net for hosts that forget.
     *
     * When no activity is set, or the stored activity is not in at least the `STARTED`
     * state, [track] falls back to `Sprig.track` and in-app experiences are not presented.
     */
    fun setFragmentActivity(activity: FragmentActivity?) {
        currentActivity = activity
    }

    override fun onActivityDestroyed(activity: Activity) {
        // Only clear when the destroyed activity is the one the host registered via setFragmentActivity.
        // Other activities in the app may be destroyed independently and must not reset that reference.
        if (activity == currentActivity) {
            currentActivity = null
        }
    }

    override fun teardown() {
        super.teardown()
        loggingListener?.let { listener ->
            sprig?.removeEventListener(EventName.LOGGING_EVENT, listener)
        }
        loggingListener = null
        (analytics as? AndroidAnalytics)?.removeLifecycleObserver(this)
        currentActivity = null
        sprig = null
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
private fun registerSprigLogging(logger: Logger, rudderLogLevel: Logger.LogLevel): EventListener? {
    if (rudderLogLevel == Logger.LogLevel.NONE) return null

    val listener = EventListener { event ->
        val message = "SprigIntegration: ${event.logMessage}"
        when (event.logLevel) {
            SprigLoggingLevel.DEBUG -> logger.debug(message)
            SprigLoggingLevel.INFO -> logger.info(message)
            SprigLoggingLevel.WARNING -> logger.warn(message)
            SprigLoggingLevel.ERROR, SprigLoggingLevel.CRITICAL -> logger.error(message)
        }
    }
    Sprig.addEventListener(EventName.LOGGING_EVENT, listener)
    return listener
}
