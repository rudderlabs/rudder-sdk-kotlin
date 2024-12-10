package com.rudderstack.android.android.plugins.screenrecording

import android.app.Activity
import com.rudderstack.android.android.Configuration
import com.rudderstack.android.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.android.android.utils.addLifecycleObserver
import com.rudderstack.android.android.utils.automaticProperty
import com.rudderstack.android.android.utils.removeLifecycleObserver
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.android.android.Analytics as AndroidAnalytics

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
