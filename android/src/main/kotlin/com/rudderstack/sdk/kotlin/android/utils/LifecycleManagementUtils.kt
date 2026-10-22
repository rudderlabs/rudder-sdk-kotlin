package com.rudderstack.sdk.kotlin.android.utils

import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ObserverDecision
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ProcessLifecycleObserver
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi

/**
 * Adds the given [observer] to the list of observers that will be notified of lifecycle events.
 */
@InternalRudderApi
fun Analytics.addLifecycleObserver(observer: ActivityLifecycleObserver) {
    activityLifecycleManagementPlugin.addObserver(observer)
}

/** Adds a late [observer], which first gets each started activity. Remove it with the `WithCurrentState` variant. */
@InternalRudderApi
fun Analytics.addLifecycleObserverWithCurrentState(observer: ActivityLifecycleObserver) {
    val integration = observer as? IntegrationPlugin
    if (integration == null) {
        activityLifecycleManagementPlugin.addObserverWithCurrentState(observer)
        return
    }
    // Events only while the destination is ready, so consent and the dashboard switch apply.
    activityLifecycleManagementPlugin.addObserverWithCurrentState(observer) {
        when {
            !integration.isPluginSetup -> ObserverDecision.FORGET
            integration.isDestinationReady -> ObserverDecision.OBSERVE
            else -> ObserverDecision.WAIT
        }
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

/** Removes an [observer] added with [addLifecycleObserverWithCurrentState], and sends the pause and stop it is owed. */
@InternalRudderApi
fun Analytics.removeLifecycleObserverWithCurrentState(observer: ActivityLifecycleObserver) {
    activityLifecycleManagementPlugin.removeObserverWithCurrentState(observer)
}

internal fun Analytics.removeLifecycleObserver(observer: ProcessLifecycleObserver) {
    processLifecycleManagementPlugin.removeObserver(observer)
}
