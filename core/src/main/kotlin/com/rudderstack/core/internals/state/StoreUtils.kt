package com.rudderstack.core.internals.state

/**
 * Represents a state in the application.
 * All state classes should implement this interface.
 */
interface State

/**
 * Represents an action that can be dispatched to the store.
 * All action classes should implement this interface.
 */
interface Action

/**
 * Middleware provides a way to intercept actions before they reach the reducer.
 * Middleware can be used for logging, modifying actions, or handling side effects.
 *
 * @param State The type of the state.
 * @param Action The type of the action.
 */
interface Middleware<State, Action> {
    /**
     * Intercepts and processes an action before it reaches the reducer.
     *
     * @param state The current state.
     * @param action The action being dispatched.
     * @param dispatch A function to dispatch actions.
     * @param next A function to pass the action to the next middleware in the chain.
     * @return The processed action.
     */
    operator fun invoke(state: State, action: Action, dispatch: Dispatch<Action>, next: Next<State, Action>): Action

    /**
     * Releases any resources or cleanup when the middleware is no longer needed.
     */
    fun dispose() {}
}

/**
 * Reducer is a pure function that takes the current state and an action and returns a new state.
 * It is responsible for updating the state based on the action.
 *
 * @param State The type of the state.
 * @param Action The type of the action.
 */
typealias Reducer<State, Action> = (currentState: State, action: Action) -> State

/**
 * Represents the next function in the middleware chain.
 * It is used to pass control to the next middleware or to return the final processed action.
 *
 * @param State The type of the state.
 * @param Action The type of the action.
 */
typealias Next<State, Action> = (state: State, action: Action, dispatch: Dispatch<Action>) -> Action

/**
 * Represents a function to dispatch actions to the store.
 * Dispatching an action triggers the reducer and updates the state.
 *
 * @param Action The type of the action.
 */
typealias Dispatch<Action> = (action: Action) -> Unit


/**
 * Represents a store that holds the state of the application.
 * It allows subscribing to state changes and dispatching actions to update the state.
 *
 * @param S The type of the state managed by the store.
 * @param A The type of the action that can be dispatched to the store.
 */
interface Store<S : State, A : Action> {

    /**
     * Subscribes to state changes in the store.
     *
     * @param subscription A function that will be called with the current state and a dispatch function whenever the state changes.
     * @return A function that can be called to unsubscribe from state changes.
     */
    fun subscribe(subscription: Subscription<S, A>): Unsubscribe
}

/**
 * Represents a function that gets called whenever the state in the store changes.
 *
 * @param State The type of the state.
 * @param Action The type of the action that can be dispatched.
 * @param currentState The current state of the store.
 * @param dispatch A function to dispatch actions to the store.
 */
typealias Subscription<State, Action> = (currentState: State, dispatch: Dispatch<Action>) -> Unit

/**
 * Represents a function that can be called to unsubscribe from state changes in the store.
 */
typealias Unsubscribe = () -> Unit
