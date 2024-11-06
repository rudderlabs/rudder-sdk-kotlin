package com.rudderstack.android.sdk.utils

import com.rudderstack.android.sdk.Analytics
import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ProcessLifecycleObserver

internal fun Analytics.addLifecycleObserver(observer: ActivityLifecycleObserver) {
    lifeCycleManagementPlugin.addObserver(observer)
}

internal fun Analytics.addLifecycleObserver(observer: ProcessLifecycleObserver) {
    lifeCycleManagementPlugin.addObserver(observer)
}

internal fun Analytics.removeLifecycleObserver(observer: ActivityLifecycleObserver) {
    lifeCycleManagementPlugin.removeObserver(observer)
}

internal fun Analytics.removeLifecycleObserver(observer: ProcessLifecycleObserver) {
    lifeCycleManagementPlugin.removeObserver(observer)
}
