package com.rudderstack.sdk.kotlin.core.internals.connectivity

import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi

/**
 * Interface for observing network availability.
 */
@InternalRudderApi
interface ConnectivityObserver : ConnectivitySubscriber

/**
 * Base class for connectivity observers.
 */
@InternalRudderApi
abstract class BaseConnectivityObserver : ConnectivitySubscriber {

    /**
     * The connectivity observer to be used.
     */
    protected abstract val connectivityObserver: ConnectivityObserver

    /**
     * Observes the network availability and notifies the subscriber immediately if the network is available.
     * Otherwise, it waits for the network to be available and then notifies the subscriber.
     *
     * **NOTE**: Subscriber are notified exactly once.
     *
     * @param subscriber The subscriber to be notified when the network is available.
     */
    override suspend fun immediateNotifyOrObserveConnectivity(subscriber: suspend () -> Unit) {
        connectivityObserver.immediateNotifyOrObserveConnectivity(subscriber)
    }
}
