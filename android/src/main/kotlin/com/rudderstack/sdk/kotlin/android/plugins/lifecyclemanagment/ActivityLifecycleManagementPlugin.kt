package com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.rudderstack.sdk.kotlin.android.utils.queueOnMainThread
import com.rudderstack.sdk.kotlin.android.utils.runOnMainThread
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.coroutines.DelicateCoroutinesApi
import org.jetbrains.annotations.VisibleForTesting
import java.util.concurrent.CopyOnWriteArrayList
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics
import com.rudderstack.sdk.kotlin.android.Configuration as AndroidConfiguration

@OptIn(DelicateCoroutinesApi::class)
internal class ActivityLifecycleManagementPlugin : Plugin, Application.ActivityLifecycleCallbacks {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Utility
    override lateinit var analytics: Analytics

    private lateinit var application: Application

    @VisibleForTesting
    internal val activityObservers = CopyOnWriteArrayList<ActivityLifecycleObserver>()

    private val startedActivities = StartedActivities()

    private val currentStateObservers = CurrentStateObservers(activityObservers, startedActivities) { block ->
        (analytics as? AndroidAnalytics)?.queueOnMainThread(block)
    }

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
        (analytics as? AndroidAnalytics)?.queueOnMainThread {
            application.unregisterActivityLifecycleCallbacks(this)
        }
    }

    internal fun addObserver(observer: ActivityLifecycleObserver) {
        activityObservers.add(observer)
    }

    internal fun removeObserver(observer: ActivityLifecycleObserver) {
        activityObservers.remove(observer)
    }

    /** Adds [observer] while [policy] decides to observe; on each add, it first gets each started activity. */
    internal fun addObserverWithCurrentState(observer: ActivityLifecycleObserver, policy: ObserverPolicy) {
        currentStateObservers.add(observer, policy)
    }

    /** Asks the policy of [observer] again. Call it when the policy can decide differently. */
    internal fun refreshObserver(observer: ActivityLifecycleObserver) {
        currentStateObservers.refresh(observer)
    }

    /** Removes [observer]. It last gets the pause and the stop of each activity that is started now. */
    internal fun removeObserverWithCurrentState(observer: ActivityLifecycleObserver) {
        currentStateObservers.remove(observer)
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        activityObservers.forEach { it.onActivityCreated(activity, bundle) }
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities.onStarted(activity)
        activityObservers.forEach { it.onActivityStarted(activity) }
    }

    override fun onActivityResumed(activity: Activity) {
        startedActivities.onResumed(activity)
        activityObservers.forEach { it.onActivityResumed(activity) }
    }

    override fun onActivityPaused(activity: Activity) {
        startedActivities.onPaused(activity)
        activityObservers.forEach { it.onActivityPaused(activity) }
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities.onStopped(activity)
        activityObservers.forEach { it.onActivityStopped(activity) }
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        activityObservers.forEach { it.onActivitySaveInstanceState(activity, bundle) }
    }

    override fun onActivityDestroyed(activity: Activity) {
        activityObservers.forEach { it.onActivityDestroyed(activity) }
    }
}
