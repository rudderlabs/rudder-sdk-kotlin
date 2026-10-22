package com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment

import android.app.Activity
import java.util.WeakHashMap

/** The activities that are started now (several with split screen), and whether each is resumed. Main thread only. */
internal class StartedActivities {

    private val states = WeakHashMap<Activity, ActivityState>()

    fun onStarted(activity: Activity) {
        states[activity] = ActivityState.STARTED
    }

    fun onResumed(activity: Activity) {
        states[activity] = ActivityState.RESUMED
    }

    fun onPaused(activity: Activity) {
        if (activity in states) states[activity] = ActivityState.STARTED
    }

    fun onStopped(activity: Activity) {
        states.remove(activity)
    }

    /** Tells [observer] that each started activity started, and that each resumed activity resumed. */
    fun replayStartTo(observer: ActivityLifecycleObserver) {
        states.toList().forEach { (activity, state) ->
            observer.onActivityStarted(activity)
            if (state == ActivityState.RESUMED) observer.onActivityResumed(activity)
        }
    }

    /** Tells [observer] that each resumed activity paused, and that each started activity stopped. */
    fun replayStopTo(observer: ActivityLifecycleObserver) {
        states.toList().forEach { (activity, state) ->
            if (state == ActivityState.RESUMED) observer.onActivityPaused(activity)
            observer.onActivityStopped(activity)
        }
    }

    private enum class ActivityState { STARTED, RESUMED }
}
