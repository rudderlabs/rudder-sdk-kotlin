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
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
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

    // Each started activity, and whether it is resumed. Several can be started: a translucent activity on top, or split
    // screen. Read and written on the main thread only, where Android delivers the activity callbacks.
    private val startedActivities = WeakHashMap<Activity, Boolean>()

    // The latest request for each observer that tracks the current state. Any thread writes it; the main thread applies it.
    private val observerRequests = ConcurrentHashMap<ActivityLifecycleObserver, ObserverRequest>()

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

    /**
     * Asks for [observer] to get the activity events. When the observer is added, it first gets each activity that is
     * started now: started, and resumed if it is.
     *
     * An observer that registers after the first activity started would otherwise miss that activity.
     *
     * The main thread applies the latest request of each observer. It reads [isActive] when it applies the request, and
     * adds the observer only when [isActive] returns `true` then. So requests from several threads, in any order, end in
     * the state that the latest request asks for.
     */
    internal fun addObserverWithCurrentState(observer: ActivityLifecycleObserver, isActive: () -> Boolean = { true }) {
        observerRequests[observer] = ObserverRequest(isActive)
        applyRequestOnMainThread(observer)
    }

    internal fun removeObserver(observer: ActivityLifecycleObserver) {
        activityObservers.remove(observer)
    }

    /**
     * Asks for [observer] to stop getting the activity events. When the observer is removed, it last gets each activity
     * that is started now: paused, if it is resumed, and stopped.
     *
     * An observer removed while an activity is on screen would otherwise never get the matching pause and stop.
     */
    internal fun removeObserverWithCurrentState(observer: ActivityLifecycleObserver) {
        observerRequests.remove(observer)
        applyRequestOnMainThread(observer)
    }

    // Shutdown cancels the analytics scope right after teardown, so the apply must survive that cancel.
    private fun applyRequestOnMainThread(observer: ActivityLifecycleObserver) {
        (analytics as? AndroidAnalytics)?.queueOnMainThread { applyRequest(observer) }
    }

    // Main thread only. Each call moves the observer to the state that its latest request asks for, so it is idempotent.
    private fun applyRequest(observer: ActivityLifecycleObserver) {
        val request = observerRequests[observer]
        val isWanted = request?.isActive?.invoke() == true
        // Removes only this request, so a newer request from another thread survives.
        if (request != null && !isWanted) observerRequests.remove(observer, request)

        val isAdded = observer in activityObservers
        val activities = startedActivities.entries.map { it.key to it.value }
        when {
            isWanted && !isAdded -> {
                activityObservers.add(observer)
                activities.forEach { (activity, isResumed) ->
                    observer.onActivityStarted(activity)
                    if (isResumed) observer.onActivityResumed(activity)
                }
            }

            !isWanted && isAdded -> {
                activityObservers.remove(observer)
                activities.forEach { (activity, isResumed) ->
                    if (isResumed) observer.onActivityPaused(activity)
                    observer.onActivityStopped(activity)
                }
            }
        }
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        activityObservers.forEach { it.onActivityCreated(activity, bundle) }
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities[activity] = false
        activityObservers.forEach { it.onActivityStarted(activity) }
    }

    override fun onActivityResumed(activity: Activity) {
        startedActivities[activity] = true
        activityObservers.forEach { it.onActivityResumed(activity) }
    }

    override fun onActivityPaused(activity: Activity) {
        if (activity in startedActivities) startedActivities[activity] = false
        activityObservers.forEach { it.onActivityPaused(activity) }
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities.remove(activity)
        activityObservers.forEach { it.onActivityStopped(activity) }
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        activityObservers.forEach { it.onActivitySaveInstanceState(activity, bundle) }
    }

    override fun onActivityDestroyed(activity: Activity) {
        activityObservers.forEach { it.onActivityDestroyed(activity) }
    }
}

private class ObserverRequest(val isActive: () -> Boolean)
