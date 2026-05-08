package com.rudderstack.scenarioengine.infrastructure.mockserver

import com.rudderstack.scenarioengine.domain.helper.MockPlane
import com.rudderstack.scenarioengine.domain.step.FieldMatch
import com.rudderstack.scenarioengine.domain.step.MatchOp
import com.rudderstack.scenarioengine.domain.step.StepEventType
import com.rudderstack.scenarioengine.domain.transport.MockResponseSpec
import com.rudderstack.scenarioengine.domain.transport.MockServer
import com.rudderstack.scenarioengine.domain.transport.RecordedRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
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
 * **Transcript model.** [start] launches a long-lived collector against the underlying
 * [MockServer]'s request stream; every batch is parsed and appended to an in-memory
 * [transcript] of events. [nextEvent] / [assertNoEvent] read against the transcript rather
 * than subscribing to the live (non-replaying) request flow each call.
 *
 * Why: the underlying [MockServer] uses a `MutableSharedFlow(replay = 0)`. Without a
 * pre-existing subscriber, events emitted before the test calls `nextEvent` are silently
 * dropped — and many SDK events (cold-start lifecycle Tracks, auto-flushed Tracks emitted
 * inside an Init) fire faster than a freshly-subscribed `nextEvent` can attach. The transcript
 * is populated by the long-lived collector so a `nextEvent` call placed *after* the event
 * fired still finds it. [nextEvent] auto-advances a cursor past each match so successive
 * calls return successive events, preserving the previous "queue" semantics. [assertNoEvent]
 * snapshots the cursor at call-start so events that arrived earlier in the test don't
 * spuriously fail the assertion.
 *
 * **Step 5 scope.** [nextEvent] supports type/name filtering and [MatchOp.EQ] in the [match]
 * list — sufficient for `smoke.basic_track`. Other [MatchOp]s `TODO()` until a scenario needs
 * them; the smoke test does not exercise them. [waitForBatch] returns the next batch
 * wholesale and reads the live request flow rather than the transcript — "next" is a
 * forward-looking concept and the only call site (assertion-pack) doesn't need history.
 *
 * **Lifecycle.** [start] / [shutdown] delegate to the underlying server, so this adapter is
 * idempotent for the same reasons. [baseUrl] is only meaningful after [start].
 */
class OkHttpMockPlane(private val server: MockServer) : MockPlane {

    override val baseUrl: String
        get() = server.baseUrl

    private val transcript = mutableListOf<JsonObject>()
    private val transcriptMutex = Mutex()

    /**
     * Monotonic counter that advances every time [transcript] grows. Suspending readers wait
     * on this via `transcriptVersion.first { it > snapshot }` — `first` evaluates the predicate
     * against the *current* value first, so a writer-update racing with a reader-subscribe is
     * picked up without an explicit barrier.
     */
    private val transcriptVersion = MutableStateFlow(0)

    /**
     * Cursor used by [nextEvent]. Each successful match advances past that event's index so
     * a follow-up [nextEvent] call returns the *next* match, mirroring the queue-style
     * semantics of the previous SharedFlow-based implementation.
     */
    private var nextEventCursor = 0
    private val cursorMutex = Mutex()

    private val collectorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var collectorJob: Job? = null

    override suspend fun start() {
        server.start()
        // Subscribe immediately so events emitted before the first `nextEvent` call still
        // land in the transcript. The collector runs for the lifetime of the plane.
        collectorJob = collectorScope.launch {
            server.observeRequests()
                .filter { it.isBatchPost() }
                .collect { request ->
                    val events = parseBatch(request)
                    if (events.isEmpty()) return@collect
                    transcriptMutex.withLock {
                        transcript.addAll(events)
                        transcriptVersion.value = transcript.size
                    }
                }
        }
    }

    override suspend fun shutdown() {
        collectorJob?.cancel()
        collectorScope.cancel()
        server.shutdown()
    }

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
    ): JsonObject = cursorMutex.withLock {
        withTimeout(timeoutMs) {
            val (event, indexInTranscript) = firstEventMatching(
                fromIndex = nextEventCursor,
                predicate = { e -> e.matchesType(type) && e.matchesName(name) && matchesAll(e, match) },
            )
            nextEventCursor = indexInTranscript + 1
            event
        }
    }

    override suspend fun assertNoEvent(type: StepEventType, name: String?, windowMs: Long) {
        // Snapshot the transcript size so historical events (e.g. an earlier auto-flushed
        // Track from the same test) don't trigger a spurious failure — only events that
        // arrive *during* the window count.
        val cursorAtStart = transcriptMutex.withLock { transcript.size }
        val unwanted = withTimeoutOrNull(windowMs) {
            firstEventMatching(
                fromIndex = cursorAtStart,
                predicate = { e -> e.matchesType(type) && e.matchesName(name) },
            ).first
        }
        if (unwanted != null) {
            error("assertNoEvent($type, $name) failed: observed $unwanted within ${windowMs}ms")
        }
    }

    /**
     * Scan [transcript] from [fromIndex], returning the first event satisfying [predicate]
     * and its index. If no match exists yet, suspend until a new event is appended and re-scan.
     * Loops indefinitely — the surrounding `withTimeout` is what bounds the wait.
     */
    private suspend fun firstEventMatching(
        fromIndex: Int,
        predicate: (JsonObject) -> Boolean,
    ): Pair<JsonObject, Int> {
        var cursor = fromIndex
        while (true) {
            val (match, snapshotSize) = transcriptMutex.withLock {
                val matchedIndex = (cursor until transcript.size).firstOrNull { predicate(transcript[it]) }
                val matchedEvent = matchedIndex?.let { transcript[it] }
                Pair(matchedEvent?.let { it to matchedIndex }, transcript.size)
            }
            if (match != null) return match
            cursor = snapshotSize
            // Wait for transcript to grow beyond the position we just scanned. `first` re-checks
            // the current StateFlow value at subscription time, so a writer that updated version
            // between our unlock and our await is observed without a missed-wakeup race.
            transcriptVersion.first { it > cursor }
        }
        @Suppress("UNREACHABLE_CODE")
        error("unreachable")
    }

    override suspend fun peekEvents(limit: Int): List<JsonObject> = transcriptMutex.withLock {
        if (limit <= 0 || transcript.isEmpty()) return@withLock emptyList()
        // Tail: the last `limit` events. takeLast on a copy keeps the snapshot independent
        // of further transcript mutations after the mutex is released.
        transcript.takeLast(limit).toList()
    }

    override fun installRoute(path: String, response: MockResponseSpec) {
        server.installRoute(path, response)
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
