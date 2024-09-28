package com.rudderstack.kotlin.internals.statemanagement

internal class SubscriptionManager<S : State, A : Action> {
    private val subscriptions = mutableListOf<Subscription<S, A>>()

    /**
     * Adds a new subscription to the store and notifies the new subscriber
     * with the current state.
     *
     * @param state The current state of the store.
     * @param dispatch The function to dispatch actions.
     * @param subscription The subscriber function to be notified of state changes.
     */
    fun addSubscription(state: S, dispatch: Dispatch<A>, subscription: Subscription<S, A>) {
        subscriptions.add(subscription)
        subscription(state, dispatch) // Notify immediately
    }

    /**
     * Removes a subscription from the store.
     *
     * @param subscription The subscription to remove.
     */
    fun removeSubscription(subscription: Subscription<S, A>) {
        subscriptions.remove(subscription)
    }

    /**
     * Notifies all active subscribers with the latest state and dispatch function.
     */
    fun notifySubscribers(state: S, dispatch: Dispatch<A>) {
        subscriptions.forEach { it(state, dispatch) }
    }

    /**
     * Check if there are any active subscriptions.
     */
    fun hasSubscriptions(): Boolean = subscriptions.isNotEmpty()
}
