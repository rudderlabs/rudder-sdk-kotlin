package com.rudderstack.android.sdk.utils

import com.rudderstack.android.sdk.Analytics
import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ProcessLifecycleObserver

internal fun Analytics.addLifecycleObserver(observer: ActivityLifecycleObserver) {
    activityLifecycleManagementPlugin.addObserver(observer)
}

internal fun Analytics.addLifecycleObserver(observer: ProcessLifecycleObserver) {
    processLifecycleManagementPlugin.addObserver(observer)
}

internal fun Analytics.removeLifecycleObserver(observer: ActivityLifecycleObserver) {
    activityLifecycleManagementPlugin.removeObserver(observer)
}

internal fun Analytics.removeLifecycleObserver(observer: ProcessLifecycleObserver) {
    processLifecycleManagementPlugin.removeObserver(observer)
}
