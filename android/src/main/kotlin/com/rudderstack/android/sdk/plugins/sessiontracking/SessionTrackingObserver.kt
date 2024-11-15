package com.rudderstack.android.sdk.plugins.sessiontracking

import android.app.Activity
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ProcessLifecycleObserver
import java.util.concurrent.atomic.AtomicBoolean

internal class SessionTrackingObserver(
    private val plugin: SessionTrackingPlugin
) : ProcessLifecycleObserver, ActivityLifecycleObserver {

    @VisibleForTesting
    internal val isSessionAlreadyUpdated = AtomicBoolean(true)

    override fun onCreate(owner: LifecycleOwner) { updateSession() }

    override fun onStart(owner: LifecycleOwner) { updateSession() }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) { updateSession() }

    override fun onActivityStarted(activity: Activity) { updateSession() }

    override fun onStop(owner: LifecycleOwner) {
        isSessionAlreadyUpdated.set(false)
        plugin.updateLastActivityTime()
    }

    private fun updateSession() {
        if (isSessionAlreadyUpdated.compareAndSet(false, true)) {
            plugin.checkAndStartSessionOnForeground()
        }
    }
}
