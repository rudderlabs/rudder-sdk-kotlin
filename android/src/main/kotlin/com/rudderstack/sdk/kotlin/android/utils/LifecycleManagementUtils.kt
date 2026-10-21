package com.rudderstack.sdk.kotlin.android.utils

import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ProcessLifecycleObserver
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi

/**
 * Adds the given [observer] to the list of observers that will be notified of lifecycle events.
 */
@InternalRudderApi
fun Analytics.addLifecycleObserver(observer: ActivityLifecycleObserver) {
    activityLifecycleManagementPlugin.addObserver(observer)
}

/**
 * Adds the given [observer], then tells it about the activity that is on screen now.
 *
 * Use it for an observer that registers late, for example when the source config arrives, so it does not miss
 * the activity that started before it.
 */
@InternalRudderApi
fun Analytics.addLifecycleObserverWithCurrentState(observer: ActivityLifecycleObserver) {
    activityLifecycleManagementPlugin.addObserverWithCurrentState(observer)
}

internal fun Analytics.addLifecycleObserver(observer: ProcessLifecycleObserver) {
    processLifecycleManagementPlugin.addObserver(observer)
}

/**
 * Removes the given [observer] from the list of observers that will be notified of lifecycle events.
 */
@InternalRudderApi
fun Analytics.removeLifecycleObserver(observer: ActivityLifecycleObserver) {
    activityLifecycleManagementPlugin.removeObserver(observer)
}

/**
 * Removes the given [observer], then tells it that the activity on screen now paused and stopped.
 *
 * Use it for an observer added with [addLifecycleObserverWithCurrentState], so the destination SDK gets the matching
 * pause and stop when the observer goes away while an activity is on screen.
 */
@InternalRudderApi
fun Analytics.removeLifecycleObserverWithCurrentState(observer: ActivityLifecycleObserver) {
    activityLifecycleManagementPlugin.removeObserverWithCurrentState(observer)
}

internal fun Analytics.removeLifecycleObserver(observer: ProcessLifecycleObserver) {
    processLifecycleManagementPlugin.removeObserver(observer)
}
