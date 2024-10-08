package com.rudderstack.kotlin.sdk.internals.statemanagement

// TODO("Restrict the usage of this class for clients.")
/**
 * Default implementation of [Store].
 */
class SingleThreadStore<S : State, A : Action>(
    initialState: S,
    reducer: Reducer<S, A>,
    private val middleware: List<Middleware<S, A>> = emptyList()
) : Store<S, A> {

    private val subscriptionManager = SubscriptionManager<S, A>()
    private val actionDispatcher = ActionDispatcher(initialState, reducer, middleware, subscriptionManager)

    /**
     * Adds a subscription to state changes and notifies the subscriber immediately.
     *
     * @param subscription The subscription function.
     */
    override fun subscribe(subscription: Subscription<S, A>) {
        subscriptionManager.addSubscription(actionDispatcher.state, actionDispatcher::dispatch, subscription)
    }

    /**
     * Unsubscribes from state changes.
     *
     * @param subscription The subscription to remove.
     */
    override fun unsubscribe(subscription: Subscription<S, A>) {
        subscriptionManager.removeSubscription(subscription)
        if (!subscriptionManager.hasSubscriptions()) {
            disposeMiddleware()
        }
    }

    /**
     * Dispatches an action to the store.
     *
     * @param action The action to dispatch.
     */
    override fun dispatch(action: A) {
        actionDispatcher.dispatch(action)
    }

    /**
     * Disposes of middleware resources if needed.
     */
    private fun disposeMiddleware() {
        middleware.forEach { it.dispose() }
    }
}
