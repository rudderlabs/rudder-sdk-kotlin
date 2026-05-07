package com.rudderstack.scenarioengine.domain.transport

import kotlinx.coroutines.flow.Flow

/**
 * The recording side of the mock plane.
 *
 * Records every HTTP request the SDK makes, exposes the stream to subscribers, and lets a
 * scenario install per-path response specs. The *assertion vocabulary* (waitForBatch,
 * nextEvent, assertNoEvent) lives one layer up in [com.rudderstack.scenarioengine.domain.helper.MockPlane],
 * not here — keeping them separate means a different transport (e.g. gRPC mock) can plug
 * into the same MockPlane without changing helper code.
 */
interface MockServer {
    /** The URL the SUT should be configured to send to. Stable across [start]/[shutdown] cycles. */
    val baseUrl: String

    /** Bring the server up and start listening. Idempotent. */
    suspend fun start()

    /** Stop the server and release the port. Idempotent. */
    suspend fun shutdown()

    /** A hot stream of every request received since [start], in arrival order. */
    fun observeRequests(): Flow<RecordedRequest>

    /** Override the response served at [path]. Last-write-wins per path. */
    fun installRoute(path: String, response: MockResponseSpec)
}

/**
 * One captured HTTP request. [body] is the raw text body — callers parse JSON if they expect it.
 */
data class RecordedRequest(
    val method: String,
    val path: String,
    val headers: Map<String, String>,
    val body: String,
)

/**
 * Describes how the mock server should reply for a given path.
 *
 * Sealed for exhaustiveness — adding a new response shape forces every dispatch site to handle
 * it. Three shapes cover the high-value cases without inventing a generic state machine.
 */
sealed class MockResponseSpec {
    /** A constant response. */
    data class Static(val status: Int, val body: String) : MockResponseSpec()

    /**
     * A list of static responses returned in order, one per request. Once exhausted, the
     * server falls back to its default route (typically 404). Useful for "fail twice then
     * succeed" retry scenarios.
     */
    data class Sequence(val responses: List<Static>) : MockResponseSpec()

    /** A static response held back by [delayMs] before being sent. Useful for client-timeout tests. */
    data class Delayed(val delayMs: Long, val response: Static) : MockResponseSpec()
}
