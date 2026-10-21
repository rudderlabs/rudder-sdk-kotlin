package com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.rudderstack.sdk.kotlin.android.utils.runCleanupOnMainThread
import com.rudderstack.sdk.kotlin.android.utils.runOnMainThread
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.coroutines.DelicateCoroutinesApi
import org.jetbrains.annotations.VisibleForTesting
import java.lang.ref.WeakReference
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

    // Read and written on the main thread only, where Android delivers the activity callbacks.
    private var startedActivity: WeakReference<Activity>? = null
    private var isStartedActivityResumed = false

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
        (analytics as? AndroidAnalytics)?.runCleanupOnMainThread {
            application.unregisterActivityLifecycleCallbacks(this)
        }
    }

    internal fun addObserver(observer: ActivityLifecycleObserver) {
        activityObservers.add(observer)
    }

    /**
     * Adds [observer], then tells it about the activity that is on screen now: started, and resumed if it is.
     *
     * An observer that registers after the first activity started would otherwise miss that activity.
     */
    internal fun addObserverWithCurrentState(observer: ActivityLifecycleObserver) {
        (analytics as? AndroidAnalytics)?.runOnMainThread {
            activityObservers.add(observer)
            startedActivity?.get()?.let { activity ->
                observer.onActivityStarted(activity)
                if (isStartedActivityResumed) observer.onActivityResumed(activity)
            }
        }
    }

    internal fun removeObserver(observer: ActivityLifecycleObserver) {
        activityObservers.remove(observer)
    }

    /**
     * Removes [observer], then tells it that the activity on screen now paused, if it is resumed, and stopped.
     *
     * An observer removed while an activity is on screen would otherwise never get the matching pause and stop.
     */
    internal fun removeObserverWithCurrentState(observer: ActivityLifecycleObserver) {
        (analytics as? AndroidAnalytics)?.runCleanupOnMainThread {
            if (!activityObservers.remove(observer)) return@runCleanupOnMainThread
            startedActivity?.get()?.let { activity ->
                if (isStartedActivityResumed) observer.onActivityPaused(activity)
                observer.onActivityStopped(activity)
            }
        }
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        activityObservers.forEach { it.onActivityCreated(activity, bundle) }
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivity = WeakReference(activity)
        isStartedActivityResumed = false
        activityObservers.forEach { it.onActivityStarted(activity) }
    }

    override fun onActivityResumed(activity: Activity) {
        startedActivity = WeakReference(activity)
        isStartedActivityResumed = true
        activityObservers.forEach { it.onActivityResumed(activity) }
    }

    override fun onActivityPaused(activity: Activity) {
        if (startedActivity?.get() === activity) isStartedActivityResumed = false
        activityObservers.forEach { it.onActivityPaused(activity) }
    }

    override fun onActivityStopped(activity: Activity) {
        if (startedActivity?.get() === activity) {
            startedActivity = null
            isStartedActivityResumed = false
        }
        activityObservers.forEach { it.onActivityStopped(activity) }
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        activityObservers.forEach { it.onActivitySaveInstanceState(activity, bundle) }
    }

    override fun onActivityDestroyed(activity: Activity) {
        activityObservers.forEach { it.onActivityDestroyed(activity) }
    }
}
