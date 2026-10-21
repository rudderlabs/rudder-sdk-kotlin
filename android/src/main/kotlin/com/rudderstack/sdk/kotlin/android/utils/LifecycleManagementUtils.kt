package com.rudderstack.sdk.kotlin.android.utils

import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
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
 * Adds the given [observer], then tells it about each activity that is started now.
 *
 * Use it for an observer that registers late, for example when the source config arrives, so it does not miss
 * the activity that started before it. Remove the observer with [removeLifecycleObserverWithCurrentState], not with
 * [removeLifecycleObserver].
 */
@InternalRudderApi
fun Analytics.addLifecycleObserverWithCurrentState(observer: ActivityLifecycleObserver) {
    val integration = observer as? IntegrationPlugin
    if (integration == null) {
        activityLifecycleManagementPlugin.addObserverWithCurrentState(observer)
    } else {
        // Events only while the destination is ready, so consent and the dashboard switch apply. A `create` that ends
        // after the app removed the integration does not add it again.
        activityLifecycleManagementPlugin.addObserverWithCurrentState(
            observer,
            isAttached = { integration.isPluginSetup },
            isReady = { integration.isDestinationReady },
        )
    }
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
 * Removes the given [observer], then tells it that each activity that is started now paused and stopped.
 *
 * Use it for an observer added with [addLifecycleObserverWithCurrentState], so the destination SDK gets the matching
 * pause and stop when the observer goes away while an activity is on screen. Do not use it for an observer added
 * with [addLifecycleObserver].
 */
@InternalRudderApi
fun Analytics.removeLifecycleObserverWithCurrentState(observer: ActivityLifecycleObserver) {
    activityLifecycleManagementPlugin.removeObserverWithCurrentState(observer)
}

internal fun Analytics.removeLifecycleObserver(observer: ProcessLifecycleObserver) {
    processLifecycleManagementPlugin.removeObserver(observer)
}
