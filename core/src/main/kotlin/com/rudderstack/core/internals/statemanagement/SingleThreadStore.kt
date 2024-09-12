package com.rudderstack.core.internals.statemanagement

import org.jetbrains.annotations.VisibleForTesting

internal class SingleThreadStore<S : State, A : Action>(
    private var state: S,
    private val reducer: Reducer<S, A>,
    private val middleware: List<Middleware<S, A>> = emptyList()
) : Store<S, A> {

    @VisibleForTesting
    private val subscriptions = mutableListOf<Subscription<S, A>>()

    override fun subscribe(subscription: Subscription<S, A>): Unsubscribe {
        subscriptions.add(subscription)
        subscription(state, ::dispatch)

        return {
            subscriptions.remove(subscription)
            if (subscriptions.isEmpty()) disposeMiddleware()
        }
    }

    @VisibleForTesting
    internal fun dispatch(action: A) {
        val newState = reducer(state, applyMiddleware(state, action, 0))
        if (newState != state) {
            state = newState
            subscriptions.forEach { it(state, ::dispatch) }
        }
    }

    private fun applyMiddleware(currentState: S, action: A, index: Int): A {
        return if (index < middleware.size) {
            middleware[index](currentState, action, ::dispatch) { s, a, d -> applyMiddleware(s, a, index + 1) }
        } else {
            action
        }
    }

    private fun disposeMiddleware() = middleware.forEach { it.dispose() }
}
