package com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment

import java.util.concurrent.ConcurrentHashMap

/** What an observer that tracks the current activity state should do now. */
internal enum class ObserverDecision {

    /** Get the activity events now. */
    OBSERVE,

    /** Do not get the activity events now, and keep the request for a later decision. */
    WAIT,

    /** Do not get the activity events, and drop the request. */
    FORGET,
}

/** Decides what an observer should do now. The main thread calls it each time it applies the request. */
internal fun interface ObserverPolicy {

    fun decide(): ObserverDecision
}

/** Late observers: any thread records the latest request of each one, and the main thread applies that request. */
internal class CurrentStateObservers(
    private val observers: MutableList<ActivityLifecycleObserver>,
    private val startedActivities: StartedActivities,
    private val queueOnMainThread: (() -> Unit) -> Unit,
) {

    private val requests = ConcurrentHashMap<ActivityLifecycleObserver, Request>()

    fun add(observer: ActivityLifecycleObserver, policy: ObserverPolicy) {
        requests[observer] = Request(policy)
        queueApply(observer)
    }

    /** Applies the request of [observer] again. Ignores an observer that has no request. */
    fun refresh(observer: ActivityLifecycleObserver) {
        if (requests.containsKey(observer)) queueApply(observer)
    }

    fun remove(observer: ActivityLifecycleObserver) {
        requests.remove(observer)
        queueApply(observer)
    }

    private fun queueApply(observer: ActivityLifecycleObserver) {
        queueOnMainThread { applyLatestRequest(observer) }
    }

    // Main thread only. A repeated call changes nothing, because it moves the observer to the latest request.
    private fun applyLatestRequest(observer: ActivityLifecycleObserver) {
        val request = requests[observer]
        val decision = request?.policy?.decide() ?: ObserverDecision.FORGET
        // Removes only this request, so a newer request from another thread survives.
        if (request != null && decision == ObserverDecision.FORGET) requests.remove(observer, request)

        val isObserving = observer in observers
        when {
            decision == ObserverDecision.OBSERVE && !isObserving -> {
                observers += observer
                startedActivities.replayStartTo(observer)
            }

            decision != ObserverDecision.OBSERVE && isObserving -> {
                observers -= observer
                startedActivities.replayStopTo(observer)
            }
        }
    }

    // Compared by identity, so each request is a new object.
    private class Request(val policy: ObserverPolicy)
}
