package com.rudderstack.sdk.kotlin.core.internals.models.connectivity

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val CURRENT_STATE = false
class ConnectivityStateTest {

    @Test
    fun `when the default connection state is set, then it should reduce to true`() {
        val result = ConnectivityState.SetDefaultStateAction()

        assertTrue(result.reduce(currentState = CURRENT_STATE))
    }

    @Test
    fun `when connection state is enabled, then it should reduce to true`() {
        val result = ConnectivityState.EnableConnectivityAction()

        assertTrue(result.reduce(currentState = CURRENT_STATE))
    }

    @Test
    fun `when connection state is disable, then it should reduce to false`() {
        val result = ConnectivityState.DisableConnectivityAction()

        assertFalse(result.reduce(currentState = CURRENT_STATE))
    }
}
