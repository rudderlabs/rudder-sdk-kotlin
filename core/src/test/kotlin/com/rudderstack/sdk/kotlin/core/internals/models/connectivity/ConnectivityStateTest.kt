package com.rudderstack.sdk.kotlin.core.internals.models.connectivity

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test

private const val CURRENT_STATE = false
class ConnectivityStateTest {

    private lateinit var connectivityState: ConnectivityState

    @Before
    fun setup() {
        connectivityState = ConnectivityState()
    }

    @Test
    fun `when the default connection state is set, then it should reduce to true`() {
        val result = ConnectivityState.SetDefaultStateAction()

        assertTrue(result.reduce(currentState = CURRENT_STATE))
    }

    @Test
    fun `when connection state is toggled to true, then it should reduce to true`() {
        val result = ConnectivityState.ToggleStateAction(true)

        assertTrue(result.reduce(currentState = CURRENT_STATE))
    }

    @Test
    fun `when connection state is toggled to false, then it should reduce to false`() {
        val result = ConnectivityState.ToggleStateAction(false)

        assertFalse(result.reduce(currentState = CURRENT_STATE))
    }
}
