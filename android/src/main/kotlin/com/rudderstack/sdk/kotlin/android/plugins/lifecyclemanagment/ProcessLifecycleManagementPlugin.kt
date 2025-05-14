package com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.rudderstack.sdk.kotlin.android.utils.runOnMainThread
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics

@OptIn(DelicateCoroutinesApi::class)
internal class ProcessLifecycleManagementPlugin : Plugin, DefaultLifecycleObserver {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Utility
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
        (analytics as? AndroidAnalytics)?.runOnMainThread {
            withContext(NonCancellable) {
                lifecycle.removeObserver(this@ProcessLifecycleManagementPlugin)
            }
        }
    }

    internal fun addObserver(observer: ProcessLifecycleObserver) {
        processObservers.add(observer)
    }

    internal fun removeObserver(observer: ProcessLifecycleObserver) {
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
