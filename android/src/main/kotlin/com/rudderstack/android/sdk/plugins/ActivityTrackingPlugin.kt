package com.rudderstack.android.sdk.plugins

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.rudderstack.android.sdk.Configuration
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import kotlin.properties.Delegates

// plugin to track activity screen events.
internal class ActivityTrackingPlugin : Plugin, Application.ActivityLifecycleCallbacks {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Manual

    override lateinit var analytics: Analytics

    private lateinit var application: Application
    private var trackActivities by Delegates.notNull<Boolean>()

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        (analytics.configuration as? Configuration)?.let { config ->
            trackActivities = config.trackActivities
            if (trackActivities) {
                application = config.application
                application.registerActivityLifecycleCallbacks(this)
            }
        }
    }

    override fun teardown() {
        super.teardown()
        if (trackActivities) {
            application.unregisterActivityLifecycleCallbacks(this)
        }
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        // NO-OP
    }

    override fun onActivityStarted(activity: Activity) {
        trackActivity(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        // NO-OP
    }

    override fun onActivityPaused(activity: Activity) {
        // NO-OP
    }

    override fun onActivityStopped(activity: Activity) {
        // NO-OP
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        // NO-OP
    }

    override fun onActivityDestroyed(activity: Activity) {
        // NO-OP
    }

    private fun trackActivity(activity: Activity) {
        analytics.screen(screenName = activity.localClassName)
    }
}
