package com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment

import android.app.Activity
import android.os.Bundle
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi

/**
 * This lifecycle observer interface can be implemented by any class to observe all the activities of an application.
 */
@InternalRudderApi
interface ActivityLifecycleObserver {

    /**
     * Called when the activity is created.
     */
    fun onActivityCreated(activity: Activity, bundle: Bundle?) {}

    /**
     * Called when the activity is started.
     */
    fun onActivityStarted(activity: Activity) {}

    /**
     * Called when the activity is resumed.
     */
    fun onActivityResumed(activity: Activity) {}

    /**
     * Called when the activity is paused.
     */
    fun onActivityPaused(activity: Activity) {}

    /**
     * Called when the activity is stopped.
     */
    fun onActivityStopped(activity: Activity) {}

    /**
     * Called when the activity is saved instance state.
     */
    fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}

    /**
     * Called when the activity is destroyed.
     */
    fun onActivityDestroyed(activity: Activity) {}
}
