package com.rudderstack.android.android.utils

import com.rudderstack.android.android.Analytics
import com.rudderstack.android.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.android.android.plugins.lifecyclemanagment.ProcessLifecycleObserver

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
