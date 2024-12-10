package com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment

import android.app.Activity
import android.os.Bundle

// this lifecycle observer interface can be implemented by any class to observe all the activities of an application.
internal interface ActivityLifecycleObserver {

    fun onActivityCreated(activity: Activity, bundle: Bundle?) {}

    fun onActivityStarted(activity: Activity) {}

    fun onActivityResumed(activity: Activity) {}

    fun onActivityPaused(activity: Activity) {}

    fun onActivityStopped(activity: Activity) {}

    fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}

    fun onActivityDestroyed(activity: Activity) {}
}
