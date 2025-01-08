package com.rudderstack.sdk.kotlin.core.internals.connectivity

/**
 * Default implementation of [ConnectivityObserver].
 */
internal class DefaultConnectivityObserver : ConnectivityObserver {

    /**
     * Notifies the subscriber immediately.
     *
     * **Note**: It assumes that the network is available.
     *
     * @param subscriber The subscriber to be notified.
     */
    override suspend fun notifyImmediatelyOrSubscribe(subscriber: suspend () -> Unit) {
        subscriber()
    }
}
