package com.rudderstack.sdk.kotlin.core.internals.statemanagement

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StateTest {

    private lateinit var state: State<Int>

    @BeforeEach
    fun setUp() {
        state = State(0)
    }

    @Test
    fun `initial state should be set correctly`() = runTest {
        assertEquals(0, state.value)
    }

    @Test
    fun `given a flow action, when dispatch called, then it should update state based on action`() = runTest {
        val incrementAction = StateAction<Int> { currentState -> currentState + 5 }

        state.dispatch(incrementAction)

        assertEquals(5, state.value)
    }

    @Test
    fun `given multiple actions, when dispatch multiple actions, then they should update state correctly and synchronously`() = runTest {
        val incrementAction = StateAction<Int> { currentState -> currentState + 2 }
        val multiplyAction = StateAction<Int> { currentState -> currentState * 3 }

        state.dispatch(incrementAction)
        val afterIncrement = state.value
        assertEquals(2, afterIncrement) // (0 + 2)

        state.dispatch(multiplyAction)
        val afterMultiply = state.value
        assertEquals(6, afterMultiply) // (2 * 3)
    }

    @Test
    fun `given a collector subscribed before any dispatch, when observeDispatched collected, then the seed is skipped and only dispatched values are emitted`() =
        runTest {
            val state = State(0)
            val values = mutableListOf<Int>()

            val job = launch { state.observeDispatched().collect { values.add(it) } }
            advanceUntilIdle() // collector is subscribed; seed (0) must NOT be emitted

            state.dispatch(StateAction { 1 })
            advanceUntilIdle()
            state.dispatch(StateAction { 2 })
            advanceUntilIdle()

            assertEquals(listOf(1, 2), values)

            job.cancel()
        }

    @Test
    fun `given a value already dispatched before subscribing, when observeDispatched collected, then it immediately emits the current dispatched value and not the seed`() =
        runTest {
            val state = State(0)
            // A real value is dispatched BEFORE the collector subscribes (the late-subscriber case).
            state.dispatch(StateAction { 7 })

            val values = mutableListOf<Int>()
            val job = launch { state.observeDispatched().collect { values.add(it) } }
            advanceUntilIdle()

            // The late subscriber must receive the already-dispatched value (7), never the seed (0).
            assertEquals(listOf(7), values)

            state.dispatch(StateAction { 9 })
            advanceUntilIdle()
            assertEquals(listOf(7, 9), values)

            job.cancel()
        }
}
