package com.rudderstack.sdk.kotlin.android.plugins

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.rudderstack.sdk.kotlin.android.utils.queueOnMainThread
import com.rudderstack.sdk.kotlin.android.utils.runOnMainThread
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.coroutines.DelicateCoroutinesApi
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics

/**
 * Runs the source config refresh loop while the app is in the foreground.
 */
@OptIn(DelicateCoroutinesApi::class)
internal class SourceConfigRefreshPlugin : Plugin, DefaultLifecycleObserver {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Utility
    override lateinit var analytics: Analytics

    private lateinit var lifecycle: Lifecycle

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        lifecycle = getProcessLifecycle()
        (analytics as? AndroidAnalytics)?.runOnMainThread {
            lifecycle.addObserver(this)
        }
    }

    override fun teardown() {
        (analytics as? AndroidAnalytics)?.queueOnMainThread {
            lifecycle.removeObserver(this)
            analytics.sourceConfigManager.stopPeriodicRefresh()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        analytics.sourceConfigManager.startPeriodicRefresh()
    }

    override fun onStop(owner: LifecycleOwner) {
        analytics.sourceConfigManager.stopPeriodicRefresh()
    }

    @VisibleForTesting
    internal fun getProcessLifecycle(): Lifecycle {
        return ProcessLifecycleOwner.get().lifecycle
    }
}
