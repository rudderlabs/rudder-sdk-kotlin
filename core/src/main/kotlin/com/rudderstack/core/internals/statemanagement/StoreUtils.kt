package com.rudderstack.core.internals.statemanagement

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
     * This allows modifying actions, logging, or triggering side effects.
     *
     * @param state The current state of the store.
     * @param action The action being dispatched.
     * @param dispatch A function to dispatch additional actions to the store.
     * @param next A function to pass the action to the next middleware or directly to the reducer.
     * @return The action that should be passed to the next middleware or reducer.
     */
    operator fun invoke(state: State, action: Action, dispatch: Dispatch<Action>, next: Next<State, Action>): Action

    /**
     * Releases any resources held by the middleware.
     * This is typically called when the middleware is no longer needed, such as when the store is being disposed of.
     */
    fun dispose() {}
}

/**
 * A reducer is a pure function that takes the current state and an action, and returns the new state.
 * The reducer is responsible for applying the changes dictated by the action to the state.
 *
 * @param State The type of the state managed by the reducer.
 * @param Action The type of the action that the reducer will process.
 * @param currentState The current state of the store.
 * @param action The action that describes the change to be made.
 * @return The new state after the action has been applied.
 */
typealias Reducer<State, Action> = (currentState: State, action: Action) -> State

/**
 * The `Next` function represents the next step in the middleware chain.
 * It is used to pass control to the next middleware or directly to the reducer.
 *
 * @param State The type of the state.
 * @param Action The type of the action.
 * @param state The current state of the store.
 * @param action The action being dispatched.
 * @param dispatch A function to dispatch additional actions to the store.
 * @return The action that should be passed to the next middleware or reducer.
 */
typealias Next<State, Action> = (state: State, action: Action, dispatch: Dispatch<Action>) -> Action

/**
 * Dispatch is a function used to send actions to the store.
 * When an action is dispatched, the reducer is called to update the state.
 *
 * @param Action The type of the action to be dispatched.
 * @param action The action to be sent to the store.
 */
typealias Dispatch<Action> = (action: Action) -> Unit

/**
 * Represents a store that holds the state of the application.
 * The store allows dispatching actions to update the state and subscribing to state changes.
 *
 * @param S The type of the state managed by the store.
 * @param A The type of the action that can be dispatched to the store.
 */
interface Store<S : State, A : Action> {

    /**
     * Subscribes to state changes in the store.
     * The provided subscription function will be called whenever the state changes,
     * passing the new state and a dispatch function to the subscriber.
     *
     * @param subscription A function that will be called with the current state and a dispatch function
     *        whenever the state in the store changes.
     */
    fun subscribe(subscription: Subscription<S, A>)

    /**
     * Unsubscribes from state changes in the store.
     * This function removes the previously added subscription so that it will no longer be notified
     * of state changes.
     *
     * @param subscription The subscription function that was previously registered with the store.
     */
    fun unsubscribe(subscription: Subscription<S, A>)

    /**
     * Dispatches an action to the store. The action will be processed by the middleware and the reducer,
     * resulting in a new state if applicable.
     *
     * @param action The action to be dispatched to the store.
     */
    fun dispatch(action: A)
}

/**
 * Subscription is a function that is called whenever the state in the store changes.
 * It receives the new state and a function to dispatch actions back to the store.
 *
 * @param State The type of the state.
 * @param Action The type of the action that can be dispatched.
 * @param currentState The current state of the store.
 * @param dispatch A function to dispatch actions to the store.
 */
typealias Subscription<State, Action> = (currentState: State, dispatch: Dispatch<Action>) -> Unit
