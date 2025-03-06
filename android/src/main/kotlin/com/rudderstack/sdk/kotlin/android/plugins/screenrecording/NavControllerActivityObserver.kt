package com.rudderstack.sdk.kotlin.android.plugins.screenrecording

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.rudderstack.sdk.kotlin.android.state.NavContext
import com.rudderstack.sdk.kotlin.android.utils.runOnMainThread
import kotlinx.coroutines.DelicateCoroutinesApi
import java.util.concurrent.atomic.AtomicBoolean
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics

/*
* This class is used to attach an observer to a navController's activity, so that destination changes can be tracked
* when navigating back from a previous activity or when the app is foregrounded.
* */
@OptIn(DelicateCoroutinesApi::class)
internal class NavControllerActivityObserver(
    private val plugin: NavControllerTrackingPlugin,
    private val navContext: NavContext,
) : DefaultLifecycleObserver {

    private val isActivityGettingCreated = AtomicBoolean(true)

    internal fun find(navContext: NavContext) = navContext == this.navContext

    override fun onStart(owner: LifecycleOwner) {
        if (!isActivityGettingCreated.getAndSet(false)) {
            val currentDestination = navContext.navController.currentDestination
            if (currentDestination != null) {
                plugin.makeAutomaticScreenEvent(currentDestination)
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        plugin.removeContextAndObserver(navContext)
    }

    internal fun removeObserver() {
        (plugin.analytics as? AndroidAnalytics)?.runOnMainThread {
            activityLifecycle()?.removeObserver(this)
        }
    }

    internal fun addObserver() {
        (plugin.analytics as? AndroidAnalytics)?.runOnMainThread {
            activityLifecycle()?.addObserver(this)
        }
    }

    @VisibleForTesting
    internal fun activityLifecycle(): Lifecycle? {
        return (navContext.callingActivity as? LifecycleOwner)?.lifecycle
    }
}
