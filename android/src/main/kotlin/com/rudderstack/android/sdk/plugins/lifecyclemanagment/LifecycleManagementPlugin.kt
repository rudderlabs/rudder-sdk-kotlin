package com.rudderstack.android.sdk.plugins.lifecyclemanagment

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import com.rudderstack.android.sdk.Configuration as AndroidConfiguration

@DelicateCoroutinesApi
private val MAIN_DISPATCHER = Dispatchers.Main.immediate

@Suppress("TooManyFunctions")
internal class LifecycleManagementPlugin : Plugin, Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Manual
    override lateinit var analytics: Analytics

    private lateinit var application: Application
    private lateinit var lifecycle: Lifecycle

    private val activityObservers = CopyOnWriteArrayList<ActivityLifecycleObserver>()
    private val processObservers = CopyOnWriteArrayList<ProcessLifecycleObserver>()

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        (analytics.configuration as? AndroidConfiguration)?.let { config ->

            application = config.application
            lifecycle = getProcessLifecycle()

            runOnMainThread {
                application.registerActivityLifecycleCallbacks(this)
                lifecycle.addObserver(this)
            }
        }
    }

    override fun teardown() {
        super.teardown()
        activityObservers.clear()
        processObservers.clear()
        runOnMainThread {
            application.unregisterActivityLifecycleCallbacks(this)
            lifecycle.removeObserver(this)
        }
    }

    fun addObserver(observer: ActivityLifecycleObserver) {
        activityObservers.add(observer)
    }

    fun addObserver(observer: ProcessLifecycleObserver) {
        processObservers.add(observer)
    }

    fun removeObserver(observer: ActivityLifecycleObserver) {
        activityObservers.remove(observer)
    }

    fun removeObserver(observer: ProcessLifecycleObserver) {
        processObservers.remove(observer)
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

    override fun onCreate(owner: LifecycleOwner) {
        processObservers.forEach { it.onCreate(owner) }
    }

    override fun onStart(owner: LifecycleOwner) {
        processObservers.forEach { it.onStart(owner) }
    }

    override fun onResume(owner: LifecycleOwner) {
        processObservers.forEach { it.onResume(owner) }
    }

    override fun onPause(owner: LifecycleOwner) {
        processObservers.forEach { it.onPause(owner) }
    }

    override fun onStop(owner: LifecycleOwner) {
        processObservers.forEach { it.onStop(owner) }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        processObservers.forEach { it.onDestroy(owner) }
    }

    @VisibleForTesting
    internal fun getProcessLifecycle(): Lifecycle {
        return ProcessLifecycleOwner.get().lifecycle
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun runOnMainThread(block: () -> Unit) = with(analytics) {
        analyticsScope.launch(MAIN_DISPATCHER) {
            block()
        }
    }
}
