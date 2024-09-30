package com.rudderstack.kotlin.sdk.internals.statemanagement

import com.rudderstack.kotlin.sdk.internals.statemanagement.TestAction.Companion.provideAction
import com.rudderstack.kotlin.sdk.internals.statemanagement.TestState.Companion.provideInitialState
import com.rudderstack.kotlin.sdk.internals.statemanagement.TestState.Companion.provideState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SingleThreadStoreTest {

    @Test
    fun `given subscribers exists, when subscribed, then should br notified with initial state`() {
        val store = SingleThreadStore(provideInitialState(), provideReducer())
        var notifiedState: TestState? = null

        store.subscribe { state, _ ->
            notifiedState = state
        }

        assertEquals(provideInitialState(), notifiedState)
    }

    @Test
    fun `given subscribers exists, when an action is dispatched, then subscribers should be notified with an updated state`() {
        val store = SingleThreadStore(provideInitialState(), provideReducer())
        val action = provideAction(type = TestAction.ActionType.INCREMENT.name, payload = 5)

        var updatedState: TestState? = null

        store.subscribe { state, _ ->
            updatedState = state
        }

        store.dispatch(action)

        assertEquals(provideState(5), updatedState)
    }

    @Test
    fun `when middleware is applied then action should be modified`() {
        val store = SingleThreadStore(provideInitialState(), provideReducer(), listOf(provideMiddleware()))
        val action = provideAction(type = TestAction.ActionType.INCREMENT.name, payload = 5)
        var updatedState: TestState? = null

        store.subscribe { state, _ ->
            updatedState = state
        }

        store.dispatch(action)

        assertEquals(provideState(10), updatedState)
    }

    @Test
    fun `when middleware is executed, then should be executed in the correct order`() {
        val initialState = provideInitialState()
        val middlewareOrder = mutableListOf<String>()

        val middleware1: Middleware<TestState, TestAction> = object : Middleware<TestState, TestAction> {
            override fun invoke(
                state: TestState,
                action: TestAction,
                dispatch: Dispatch<TestAction>,
                next: Next<TestState, TestAction>
            ): TestAction {
                middlewareOrder.add("middleware1")
                return next(state, action, dispatch)
            }
        }

        val middleware2: Middleware<TestState, TestAction> = object : Middleware<TestState, TestAction> {
            override fun invoke(
                state: TestState,
                action: TestAction,
                dispatch: Dispatch<TestAction>,
                next: Next<TestState, TestAction>
            ): TestAction {
                middlewareOrder.add("middleware2")
                return next(state, action, dispatch)
            }
        }

        val store = SingleThreadStore(initialState, provideReducer(), listOf(middleware1, middleware2))
        store.dispatch(provideAction(type = TestAction.ActionType.INCREMENT.name, payload = 1))

        assertEquals(listOf("middleware1", "middleware2"), middlewareOrder)
    }

    @Test
    fun `when unsubscribe is called, then subscribers should be removed`() {
        val testPayload = 2
        var notifiedState: TestState? = null
        val store = SingleThreadStore(provideInitialState(), provideReducer())
        val subscription: Subscription<TestState, TestAction> = { state, dispatch ->
            notifiedState = state
        }

        store.subscribe(subscription)
        store.unsubscribe(subscription)
        store.dispatch(provideAction(type = TestAction.ActionType.INCREMENT.name, payload = testPayload))

        assertEquals(provideInitialState(), notifiedState)
    }

    @Test
    fun `when no subscribers exist, then dispose middleware`() {
        var isMiddlewareDisposed = false
        val disposableMiddleware: Middleware<TestState, TestAction> = object : Middleware<TestState, TestAction> {
            override fun invoke(
                state: TestState,
                action: TestAction,
                dispatch: Dispatch<TestAction>,
                next: Next<TestState, TestAction>
            ): TestAction {
                return next(state, action, dispatch)
            }

            override fun dispose() {
                isMiddlewareDisposed = true
            }
        }

        val store = SingleThreadStore(provideInitialState(), provideReducer(), listOf(disposableMiddleware))
        val subscription: Subscription<TestState, TestAction> = { state, dispatch -> }

        store.subscribe(subscription)
        store.unsubscribe(subscription)

        assertTrue(isMiddlewareDisposed)
    }
}


data class TestState(val value: Int) : State {
    companion object {
        fun provideInitialState() = TestState(0)
        fun provideState(value: Int) = TestState(value)
    }
}

data class TestAction(val type: String, val payload: Int) : Action {
    companion object {
        fun provideAction(type: String, payload: Int) = TestAction(type = type, payload = payload)
    }

    enum class ActionType {
        INCREMENT,
        DECREMENT,
    }
}

fun provideReducer(): Reducer<TestState, TestAction> = { state, action ->
    when (action.type) {
        TestAction.ActionType.INCREMENT.name -> state.copy(value = state.value + action.payload)
        TestAction.ActionType.DECREMENT.name -> state.copy(value = state.value - action.payload)
        else -> state
    }
}

fun provideMiddleware(): Middleware<TestState, TestAction> = object : Middleware<TestState, TestAction> {
    override fun invoke(
        state: TestState,
        action: TestAction,
        dispatch: Dispatch<TestAction>,
        next: Next<TestState, TestAction>
    ): TestAction {
        val modifiedAction = action.copy(payload = action.payload * 2)
        return next(state, modifiedAction, dispatch)
    }
}
