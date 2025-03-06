package com.rudderstack.sdk.kotlin.core.internals.statemanagement

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
        val incrementAction = FlowAction<Int> { currentState -> currentState + 5 }

        state.dispatch(incrementAction)

        assertEquals(5, state.value)
    }

    @Test
    fun `given multiple actions, when dispatch multiple actions, then they should update state correctly and synchronously`() = runTest {
        val incrementAction = FlowAction<Int> { currentState -> currentState + 2 }
        val multiplyAction = FlowAction<Int> { currentState -> currentState * 3 }

        state.dispatch(incrementAction)
        val afterIncrement = state.value
        assertEquals(2, afterIncrement) // (0 + 2)

        state.dispatch(multiplyAction)
        val afterMultiply = state.value
        assertEquals(6, afterMultiply) // (2 * 3)
    }
}
