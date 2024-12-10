package com.rudderstack.android.sdk.plugins.screenrecording

import android.app.Activity
import com.rudderstack.android.sdk.Configuration
import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.android.sdk.utils.addLifecycleObserver
import com.rudderstack.android.sdk.utils.automaticProperty
import com.rudderstack.android.sdk.utils.removeLifecycleObserver
import com.rudderstack.kotlin.core.Analytics
import com.rudderstack.kotlin.core.internals.plugins.Plugin
import com.rudderstack.android.sdk.Analytics as AndroidAnalytics

// plugin to track activity screen events.
internal class ActivityTrackingPlugin : Plugin, ActivityLifecycleObserver {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Manual

    override lateinit var analytics: Analytics

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        (analytics.configuration as? Configuration)?.let { config ->
            if (config.trackActivities) {
                (analytics as? AndroidAnalytics)?.addLifecycleObserver(this)
            }
        }
    }

    override fun teardown() {
        (analytics as? AndroidAnalytics)?.removeLifecycleObserver(this)
    }

    override fun onActivityStarted(activity: Activity) {
        analytics.screen(screenName = getActivityClassName(activity), properties = automaticProperty())
    }
}

internal fun getActivityClassName(activity: Activity): String {
    return activity.javaClass.name
}
