package com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.rudderstack.sdk.kotlin.android.utils.runOnMainThread
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
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
        (analytics as? AndroidAnalytics)?.runOnMainThread {
            withContext(NonCancellable) {
                application.unregisterActivityLifecycleCallbacks(this@ActivityLifecycleManagementPlugin)
            }
        }
    }

    internal fun addObserver(observer: ActivityLifecycleObserver) {
        activityObservers.add(observer)
    }

    internal fun removeObserver(observer: ActivityLifecycleObserver) {
        activityObservers.remove(observer)
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        activityObservers.forEach { it.onActivityCreated(activity, bundle) }
    }

    override fun onActivityStarted(activity: Activity) {
        activityObservers.forEach { it.onActivityStarted(activity) }
    }

    override fun onActivityResumed(activity: Activity) {
        activityObservers.forEach { it.onActivityResumed(activity) }
    }

    override fun onActivityPaused(activity: Activity) {
        activityObservers.forEach { it.onActivityPaused(activity) }
    }

    override fun onActivityStopped(activity: Activity) {
        activityObservers.forEach { it.onActivityStopped(activity) }
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        activityObservers.forEach { it.onActivitySaveInstanceState(activity, bundle) }
    }

    override fun onActivityDestroyed(activity: Activity) {
        activityObservers.forEach { it.onActivityDestroyed(activity) }
    }
}
