package com.rudderstack.kotlin.core.internals.statemanagement

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FlowStateTest {

    private lateinit var flowState: FlowState<Int>

    @Before
    fun setUp() {
        flowState = FlowState(0)
    }

    @Test
    fun `initial state should be set correctly`() = runTest {
        assertEquals(0, flowState.value)
    }

    @Test
    fun `given a flow action, when dispatch called, then it should update state based on action`() = runTest {
        val incrementAction = FlowAction<Int> { currentState -> currentState + 5 }

        flowState.dispatch(incrementAction)

        assertEquals(5, flowState.value)
    }

    @Test
    fun `given multiple actions, when dispatch multiple actions, then they should update state correctly and synchronously`() = runTest {
        val incrementAction = FlowAction<Int> { currentState -> currentState + 2 }
        val multiplyAction = FlowAction<Int> { currentState -> currentState * 3 }

        flowState.dispatch(incrementAction)
        val afterIncrement = flowState.value
        assertEquals(2, afterIncrement) // (0 + 2)

        flowState.dispatch(multiplyAction)
        val afterMultiply = flowState.value
        assertEquals(6, afterMultiply) // (2 * 3)
    }
}
