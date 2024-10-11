package com.rudderstack.android.sdk.plugins.screenrecording

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.rudderstack.android.sdk.state.NavContext
import com.rudderstack.android.sdk.state.NavContextState
import java.util.concurrent.atomic.AtomicBoolean

internal class NavControllerActivityObserver(
    private val plugin: ScreenRecordingPlugin,
    private val navContext: NavContext,
) : DefaultLifecycleObserver {

    private val isActivityGettingCreated = AtomicBoolean(true)

    init {
        (navContext.callingActivity as? LifecycleOwner)?.lifecycle?.addObserver(this)
    }

    fun isObserverForContext(navContext: NavContext?): Boolean {
        return this.navContext == navContext
    }

    override fun onStart(owner: LifecycleOwner) {
        if (!isActivityGettingCreated.get()) {
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
        isActivityGettingCreated.set(false)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        plugin.navContextStore.dispatch(NavContextState.RemoveNavContextAction(navContext.navController))
    }
}
