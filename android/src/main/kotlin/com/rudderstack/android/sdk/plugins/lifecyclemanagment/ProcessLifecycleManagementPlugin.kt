package com.rudderstack.android.sdk.plugins.lifecyclemanagment

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.rudderstack.android.sdk.utils.runOnMainThread
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList
import com.rudderstack.android.sdk.Analytics as AndroidAnalytics

@OptIn(DelicateCoroutinesApi::class)
internal class ProcessLifecycleManagementPlugin : Plugin, DefaultLifecycleObserver {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Manual
    override lateinit var analytics: Analytics

    private lateinit var lifecycle: Lifecycle

    @VisibleForTesting
    internal val processObservers = CopyOnWriteArrayList<ProcessLifecycleObserver>()

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        lifecycle = getProcessLifecycle()
        (analytics as? AndroidAnalytics)?.runOnMainThread {
            lifecycle.addObserver(this)
        }
    }

    override fun teardown() {
        super.teardown()
        processObservers.clear()
        (analytics as? AndroidAnalytics)?.runOnMainThread {
            withContext(NonCancellable) {
                lifecycle.removeObserver(this@ProcessLifecycleManagementPlugin)
            }
        }
    }

    fun addObserver(observer: ProcessLifecycleObserver) {
        processObservers.add(observer)
    }

    fun removeObserver(observer: ProcessLifecycleObserver) {
        processObservers.remove(observer)
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
}
