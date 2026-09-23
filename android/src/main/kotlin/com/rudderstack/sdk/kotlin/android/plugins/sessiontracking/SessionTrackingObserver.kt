package com.rudderstack.sdk.kotlin.android.plugins.sessiontracking

import android.app.Activity
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ProcessLifecycleObserver
import java.util.concurrent.atomic.AtomicBoolean

// ON_CREATE also fires on a background process start, so this observer never treats it as foreground.
internal class SessionTrackingObserver(
    private val sessionManager: SessionManager
) : ProcessLifecycleObserver, ActivityLifecycleObserver {

    @VisibleForTesting
    internal val isSessionAlreadyUpdated = AtomicBoolean(false)
    internal val isInForeground = AtomicBoolean(false)

    override fun onStart(owner: LifecycleOwner) { updateSession() }

    override fun onResume(owner: LifecycleOwner) { updateSession() }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) { updateSession() }

    override fun onActivityStarted(activity: Activity) { updateSession() }

    override fun onStop(owner: LifecycleOwner) {
        isInForeground.set(false)
        isSessionAlreadyUpdated.set(false)
        sessionManager.updateLastActivityTime()
    }

    private fun updateSession() {
        isInForeground.set(true)
        if (isSessionAlreadyUpdated.compareAndSet(false, true)) {
            sessionManager.checkAndStartSessionOnForeground()
        }
    }
}
