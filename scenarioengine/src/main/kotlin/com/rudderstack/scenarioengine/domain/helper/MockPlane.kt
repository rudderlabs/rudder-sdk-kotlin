package com.rudderstack.scenarioengine.domain.helper

import com.rudderstack.scenarioengine.domain.step.FieldMatch
import com.rudderstack.scenarioengine.domain.step.StepEventType
import com.rudderstack.scenarioengine.domain.transport.MockResponseSpec
import kotlinx.serialization.json.JsonObject

/**
 * The wire-side oracle.
 *
 * Wraps the underlying [com.rudderstack.scenarioengine.domain.transport.MockServer] with the
 * assertion vocabulary the interpreter speaks. Returns parsed JSON bodies, not raw strings —
 * the SDK's wire format is JSON, the mock plane is the matched abstraction.
 *
 * Two oracles, two questions:
 *  - This (`MockPlane`) — "What did the SDK send over the wire?"
 *  - `SpyPlugin` — "What did the SDK do internally?"
 */
interface MockPlane {
    /** The base URL the SUT should be configured to send to (data plane + control plane). */
    val baseUrl: String

    /** Bring the underlying mock server up. Idempotent. */
    suspend fun start()

    /** Tear the mock server down. Idempotent. */
    suspend fun shutdown()

    /**
     * Block until the next `POST /v1/batch` body arrives, returning its parsed events array.
     * Fails (throws) if the timeout elapses without a batch.
     */
    suspend fun waitForBatch(timeoutMs: Long): List<JsonObject>

    /**
     * Block until an event matching all of [type], [name], and the [match] predicates arrives,
     * returning its full JSON. Fails on timeout. An empty `match` list means "first event of
     * the right type/name."
     */
    suspend fun nextEvent(
        type: StepEventType,
        name: String?,
        timeoutMs: Long,
        match: List<FieldMatch>,
    ): JsonObject

    /**
     * Watch for [windowMs] and fail if any event of the given type/name arrives. Used to
     * assert *absence* — e.g. "no Track 'Application Backgrounded' fires within 2s of init."
     */
    suspend fun assertNoEvent(type: StepEventType, name: String?, windowMs: Long)

    /**
     * Read-only snapshot of the most-recent events the SDK has sent over the wire. Returns
     * the *tail* of the transcript (up to [limit] events) without advancing any consume
     * cursor — purely for debugging / "what actually arrived" introspection.
     *
     * Has no effect on subsequent [nextEvent] / [assertNoEvent] calls. Returns an empty list
     * when the transcript is empty; never blocks (does not wait for new events).
     */
    suspend fun peekEvents(limit: Int): List<JsonObject>

    /**
     * Override the response served at [path]. Use for fault injection: a `Static(500, "...")`,
     * a [MockResponseSpec.Sequence] that returns 500 then 200, or a [MockResponseSpec.Delayed]
     * to test client timeouts.
     */
    fun installRoute(path: String, response: MockResponseSpec)
}
