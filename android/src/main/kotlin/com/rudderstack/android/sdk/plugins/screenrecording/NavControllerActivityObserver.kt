package com.rudderstack.android.sdk.plugins.screenrecording

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.rudderstack.android.sdk.state.NavContext
import com.rudderstack.android.sdk.state.NavContextState
import java.util.concurrent.atomic.AtomicBoolean

/*
* This class is used to attach an observer to a navController's activity, so that destination changes can be tracked
* when navigating back from a previous activity or when the app is foregrounded.
* */
internal class NavControllerActivityObserver(
    private val plugin: NavControllerTrackingPlugin,
    private val navContext: NavContext,
) : DefaultLifecycleObserver {

    private val isActivityGettingCreated = AtomicBoolean(true)

    init {
        navContext.lifecycle()?.addObserver(this)
    }

    fun isObserverForContext(navContext: NavContext?): Boolean {
        return this.navContext == navContext
    }

    override fun onStart(owner: LifecycleOwner) {
        if (!isActivityGettingCreated.getAndSet(false)) {
            val currentController = navContext.navController
            val currentDestination = currentController.currentDestination
            if (currentDestination != null) {
                plugin.onDestinationChanged(
                    controller = currentController,
                    destination = currentDestination,
                    arguments = null
                )
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        plugin.navContextStore.dispatch(NavContextState.RemoveNavContextAction(navContext.navController))
    }
}

internal fun NavContext.lifecycle(): Lifecycle? {
    return (this.callingActivity as? LifecycleOwner)?.lifecycle
}
