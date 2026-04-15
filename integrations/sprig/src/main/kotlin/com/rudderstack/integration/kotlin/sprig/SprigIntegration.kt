package com.rudderstack.integration.kotlin.sprig

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.StandardIntegration
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.addLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.userleap.Sprig
import kotlinx.serialization.json.JsonObject
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics

private const val SPRIG_KEY = "Sprig"

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
                Sprig.configure(analytics.application.applicationContext, config.environmentId)
                sprig = Sprig
                (analytics as? AndroidAnalytics)?.addLifecycleObserver(this)
                analytics.logger.verbose("SprigIntegration: Sprig SDK initialized.")
            }
        }
    }

    override fun update(destinationConfig: JsonObject) {
        // No updatable config fields for Sprig — environmentId is set once during create().
    }

    override fun getDestinationInstance(): Any? {
        return sprig
    }

    override fun identify(payload: IdentifyEvent) {
        // TODO: Step 5 - Set user identifier and visitor attributes
    }

    override fun track(payload: TrackEvent) {
        // TODO: Step 6 - Create EventPayload and track/trackAndPresent
    }

    override fun reset() {
        // TODO: Step 7e - Call sprig.logout()
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
