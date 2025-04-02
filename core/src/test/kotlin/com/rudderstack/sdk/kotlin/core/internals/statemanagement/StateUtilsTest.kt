package com.rudderstack.sdk.kotlin.core.internals.statemanagement

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StateUtilsTest {

    @Test
    fun `given a state variable, when collected with dropping the initial state, when the default value is not collected`() =
        runTest {
            val initialState = 0
            val state = State(initialState)
            val values = mutableListOf<Int>()

            val job = launch {
                state.dropInitialState().collect { values.add(it) }
            }

            advanceUntilIdle()
            state.value = 1
            advanceUntilIdle()
            state.value = 2
            advanceUntilIdle()

            assertEquals(listOf(1, 2), values)

            job.cancel()
        }
}
