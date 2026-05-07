package com.rudderstack.scenarioengine.domain.transport

import kotlinx.coroutines.flow.Flow

/**
 * The single platform-abstraction line between driver and SUT.
 *
 * On Android this is implemented over broadcasts (commands) and a ContentProvider (state reads).
 * On iOS it would be a local HTTP server inside the SUT. Domain code does not see either —
 * the Interpreter and helpers depend on this interface, and the wiring is decided once at
 * test-run entry.
 */
interface Transport {
    /**
     * Send a fire-and-forget command to the SUT. The ack indicates the SUT received and
     * dispatched the command, not that any business effect has settled.
     */
    suspend fun sendCommand(command: String, args: Map<String, Any?>): TransportAck

    /**
     * Send a command that produces a return value, blocking until the SUT replies or [timeoutMs]
     * elapses. Returns the raw JSON body of the reply; callers parse.
     */
    suspend fun sendCommandWithResult(
        command: String,
        args: Map<String, Any?>,
        timeoutMs: Long,
    ): String

    /**
     * A hot stream of unsolicited events from the SUT — `sdkEvent` payloads from spy plugins,
     * crash notifications, etc. Subscribe before triggering the action that produces them.
     */
    fun observeEvents(): Flow<TransportEvent>

    /** Synchronously read a state field exposed by the SUT. Returns null if the field is unset. */
    suspend fun readState(field: String): String?
}

/**
 * Reply to a [Transport.sendCommand].
 *
 * @param ok true if the SUT accepted the command without error.
 * @param error On failure, a short SUT-side error description.
 */
data class TransportAck(val ok: Boolean, val error: String? = null)

/**
 * One event observed from the SUT.
 *
 * @param type One of `callback`, `result`, `error`, `sdkEvent`. Solicited events carry an [id].
 * @param id Correlation id matching the request that elicited it; null for unsolicited `sdkEvent`s.
 * @param payload Raw JSON body — the receiver decides how to parse based on [type].
 */
data class TransportEvent(val type: String, val id: String?, val payload: String)
