package com.rudderstack.scenarioengine.infrastructure.mockserver

import com.rudderstack.scenarioengine.domain.helper.MockPlane
import com.rudderstack.scenarioengine.domain.step.FieldMatch
import com.rudderstack.scenarioengine.domain.step.MatchOp
import com.rudderstack.scenarioengine.domain.step.StepEventType
import com.rudderstack.scenarioengine.domain.transport.MockResponseSpec
import com.rudderstack.scenarioengine.domain.transport.MockServer
import com.rudderstack.scenarioengine.domain.transport.RecordedRequest
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Driver-side adapter that wraps a [MockServer] (the recording layer) with the assertion
 * vocabulary the [com.rudderstack.scenarioengine.application.interpreter.SequentialInterpreter]
 * speaks: [waitForBatch], [nextEvent], [assertNoEvent].
 *
 * The Rudder SDK's wire format on `POST /v1/batch` is `{"batch":[<event>, …]}`. Each event has
 * a `type` field (`"track"`, `"screen"`, …) and — for typed events — an `event` field carrying
 * the user-supplied name. This adapter parses that shape; if the SDK's payload format changes,
 * this is the one place that needs updating.
 *
 * **Step 5 scope.** [nextEvent] supports type/name filtering and [MatchOp.EQ] in the [match]
 * list — sufficient for `smoke.basic_track`. Other [MatchOp]s `TODO()` until a scenario needs
 * them; the smoke test does not exercise them. [waitForBatch] returns the next batch wholesale.
 * [assertNoEvent] watches for the window then succeeds on silence; it fails on the first
 * matching event. Per-route response overrides delegate to [MockServer.installRoute].
 *
 * **Lifecycle.** [start] / [shutdown] delegate to the underlying server, so this adapter is
 * idempotent for the same reasons. [baseUrl] is only meaningful after [start].
 */
class OkHttpMockPlane(private val server: MockServer) : MockPlane {

    override val baseUrl: String
        get() = server.baseUrl

    override suspend fun start() = server.start()

    override suspend fun shutdown() = server.shutdown()

    override suspend fun waitForBatch(timeoutMs: Long): List<JsonObject> {
        val request = withTimeout(timeoutMs) {
            server.observeRequests().filter { it.isBatchPost() }.first()
        }
        return parseBatch(request)
    }

    override suspend fun nextEvent(
        type: StepEventType,
        name: String?,
        timeoutMs: Long,
        match: List<FieldMatch>,
    ): JsonObject = withTimeout(timeoutMs) {
        server.observeRequests()
            .filter { it.isBatchPost() }
            .mapNotNull { request -> findMatch(parseBatch(request), type, name, match) }
            .first()
    }

    override suspend fun assertNoEvent(type: StepEventType, name: String?, windowMs: Long) {
        try {
            val unwanted = withTimeout(windowMs) {
                server.observeRequests()
                    .filter { it.isBatchPost() }
                    .mapNotNull { request -> findMatch(parseBatch(request), type, name, match = emptyList()) }
                    .first()
            }
            error("assertNoEvent($type, $name) failed: observed $unwanted within ${windowMs}ms")
        } catch (_: TimeoutCancellationException) {
            // Silence — the assertion holds.
        }
    }

    override fun installRoute(path: String, response: MockResponseSpec) {
        server.installRoute(path, response)
    }

    /** Return the first event in [events] satisfying type / name / [match], or null if none. */
    private fun findMatch(
        events: List<JsonObject>,
        type: StepEventType,
        name: String?,
        match: List<FieldMatch>,
    ): JsonObject? = events.firstOrNull { event ->
        event.matchesType(type) && event.matchesName(name) && matchesAll(event, match)
    }

    /**
     * Implicit-AND over the match list. Empty list means "any event of the matching type/name".
     *
     * Step 5 supports [MatchOp.EQ] only. Other ops route to [TODO] — they arrive alongside
     * the scenario corpus that needs them.
     */
    private fun matchesAll(event: JsonObject, predicates: List<FieldMatch>): Boolean =
        predicates.all { predicate ->
            val value = resolvePath(event, predicate.path)
            when (predicate.op) {
                MatchOp.EQ -> value == predicate.value
                else -> TODO("MatchOp ${predicate.op} not supported until later steps")
            }
        }

    private fun resolvePath(json: JsonObject, path: String): JsonElement? {
        var node: JsonElement = json
        for (segment in path.split('.')) {
            val obj = node as? JsonObject ?: return null
            node = obj[segment] ?: return null
        }
        return node
    }

    private fun JsonObject.matchesType(type: StepEventType): Boolean =
        this["type"]?.jsonPrimitive?.content == type.wireValue

    private fun JsonObject.matchesName(name: String?): Boolean {
        if (name == null) return true
        val candidate = this["event"]?.jsonPrimitive?.content
            ?: this["name"]?.jsonPrimitive?.content
        return candidate == name
    }

    private fun parseBatch(request: RecordedRequest): List<JsonObject> {
        val root = runCatching { Json.parseToJsonElement(request.body).jsonObject }
            .getOrElse { return emptyList() }
        val arr: JsonArray = root["batch"]?.jsonArray ?: return emptyList()
        return arr.mapNotNull { it as? JsonObject }
    }

    private fun RecordedRequest.isBatchPost(): Boolean =
        method.equals("POST", ignoreCase = true) && path == "/v1/batch"

    private val StepEventType.wireValue: String
        get() = when (this) {
            StepEventType.TRACK -> "track"
            StepEventType.SCREEN -> "screen"
            StepEventType.IDENTIFY -> "identify"
            StepEventType.GROUP -> "group"
            StepEventType.ALIAS -> "alias"
        }
}
