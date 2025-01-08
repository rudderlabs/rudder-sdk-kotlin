package com.rudderstack.sdk.kotlin.core.internals.connectivity

import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi

/**
 * Interface for subscribing to network availability.
 */
@InternalRudderApi
interface ConnectivitySubscriber {

    /**
     * Observes the network availability and notifies the subscriber immediately if the network is available.
     * Otherwise, it waits for the network to be available and then notifies the subscriber.
     *
     * **NOTE**: Subscriber are notified exactly once.
     *
     * @param subscriber The subscriber to be notified when the network is available.
     */
    suspend fun notifyImmediatelyOrSubscribe(subscriber: suspend () -> Unit)
}
