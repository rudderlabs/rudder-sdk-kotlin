package com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment

import androidx.lifecycle.LifecycleOwner

// this lifecycle observer interface can be implemented by any class to observe the lifecycle of the application process.
internal interface ProcessLifecycleObserver {

    fun onCreate(owner: LifecycleOwner) {}

    fun onStart(owner: LifecycleOwner) {}

    fun onResume(owner: LifecycleOwner) {}

    fun onPause(owner: LifecycleOwner) {}

    fun onStop(owner: LifecycleOwner) {}

    fun onDestroy(owner: LifecycleOwner) {}
}
