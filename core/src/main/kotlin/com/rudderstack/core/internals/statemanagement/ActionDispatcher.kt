package com.rudderstack.statestore.statemanagement

class ActionDispatcher<S : State, A : Action>(
    internal var state: S,
    private val reducer: Reducer<S, A>,
    private val middleware: List<Middleware<S, A>>,
    private val subscriptionManager: SubscriptionManager<S, A>
) {

    /**
     * Dispatches an action to the store and applies middleware.
     *
     * @param action The action to dispatch.
     */
    fun dispatch(action: A) {
        val newState = reducer(state, applyMiddleware(state, action, 0))
        if (newState != state) {
            state = newState
            subscriptionManager.notifySubscribers(state, ::dispatch)
        }
    }

    /**
     * Applies the middleware in order to process the action.
     */
    private fun applyMiddleware(currentState: S, action: A, index: Int): A {
        return if (index < middleware.size) {
            middleware[index](currentState, action, ::dispatch) { s, a, _ -> applyMiddleware(s, a, index + 1) }
        } else {
            action
        }
    }
}