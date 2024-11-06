package com.rudderstack.android.sdk.plugins.lifecyclemanagment

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import com.rudderstack.android.sdk.utils.runOnMainThread
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import kotlinx.coroutines.DelicateCoroutinesApi
import java.util.concurrent.CopyOnWriteArrayList
import com.rudderstack.android.sdk.Analytics as AndroidAnalytics
import com.rudderstack.android.sdk.Configuration as AndroidConfiguration

@OptIn(DelicateCoroutinesApi::class)
@Suppress("TooManyFunctions")
internal class ActivityLifecycleManagementPlugin : Plugin, Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Manual
    override lateinit var analytics: Analytics

    private lateinit var application: Application

    private val activityObservers = CopyOnWriteArrayList<ActivityLifecycleObserver>()

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        (analytics.configuration as? AndroidConfiguration)?.let { config ->
            application = config.application
            (analytics as? AndroidAnalytics)?.runOnMainThread {
                application.registerActivityLifecycleCallbacks(this)
            }
        }
    }

    override fun teardown() {
        super.teardown()
        activityObservers.clear()
        (analytics as? AndroidAnalytics)?.runOnMainThread {
            application.unregisterActivityLifecycleCallbacks(this)
        }
    }

    fun addObserver(observer: ActivityLifecycleObserver) {
        activityObservers.add(observer)
    }

    fun removeObserver(observer: ActivityLifecycleObserver) {
        activityObservers.remove(observer)
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        activityObservers.forEach { it.onActivityCreated(activity, bundle) }
    }

    override fun onActivityStarted(activity: Activity) {
        activityObservers.forEach { it.onActivityStarted(activity) }
    }

    override fun onActivityResumed(activity: Activity) {
        activityObservers.forEach { it.onActivityResumed(activity) }
    }

    override fun onActivityPaused(activity: Activity) {
        activityObservers.forEach { it.onActivityPaused(activity) }
    }

    override fun onActivityStopped(activity: Activity) {
        activityObservers.forEach { it.onActivityStopped(activity) }
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        activityObservers.forEach { it.onActivitySaveInstanceState(activity, bundle) }
    }

    override fun onActivityDestroyed(activity: Activity) {
        activityObservers.forEach { it.onActivityDestroyed(activity) }
    }
}
