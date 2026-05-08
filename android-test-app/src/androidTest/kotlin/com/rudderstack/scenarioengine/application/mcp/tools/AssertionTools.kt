package com.rudderstack.scenarioengine.application.mcp.tools

import com.rudderstack.scenarioengine.application.mcp.Tool
import com.rudderstack.scenarioengine.application.mcp.ToolContext
import com.rudderstack.scenarioengine.application.mcp.ToolRegistry
import com.rudderstack.scenarioengine.application.mcp.errorResult
import com.rudderstack.scenarioengine.application.mcp.stepResultToToolResult
import com.rudderstack.scenarioengine.application.mcp.textResult
import com.rudderstack.scenarioengine.domain.step.Step
import com.rudderstack.scenarioengine.domain.step.StepEventType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * Five assertion tools — the shape of the wire / event-cache oracles the AI uses to verify
 * SDK behavior:
 *
 *  - `rudder.assert_next_event` — wait for the next batch carrying a matching event; return it.
 *    Wraps [Step.WaitForEvent]; the matched event populates the interpreter's `lastObservedEvent`
 *    cache so a subsequent `assert_field` call can read against it.
 *  - `rudder.assert_no_event` — verify *absence* across a window. Wraps [Step.AssertNoEvent].
 *  - `rudder.assert_field` — assert a dotted-path field on the most-recent waited-for event.
 *    Wraps [Step.AssertField].
 *  - `rudder.wait_for_batch` — wait for any batch and return its parsed events. Useful for
 *    debugging the SDK's wire format before narrowing to a specific event.
 *  - `rudder.peek_events` — read-only tail of the wire transcript. Does not advance the
 *    `assert_next_event` cursor — purely for "what arrived" introspection during debugging.
 */
internal fun registerAssertionTools(registry: ToolRegistry, ctx: ToolContext) {
    registry.register(assertNextEventTool(ctx))
    registry.register(assertNoEventTool(ctx))
    registry.register(assertFieldTool(ctx))
    registry.register(waitForBatchTool(ctx))
    registry.register(peekEventsTool(ctx))
}

private fun assertNextEventTool(ctx: ToolContext) = Tool(
    name = "rudder.assert_next_event",
    description = "Wait for the next unconsumed event matching type/name and return its full JSON. " +
        "**Consuming + ordered:** an internal cursor advances past the matched event, so a follow-up " +
        "`rudder.assert_next_event` returns the *next* event after this one — calling it twice with " +
        "the same filter does not return the same event. The matched event becomes the target for a " +
        "subsequent `rudder.assert_field` call (single most-recent slot, overwritten on the next " +
        "`assert_next_event`). Use `rudder.peek_events` to inspect the transcript without consuming.",
    inputSchema = objectSchema(
        properties = mapOf(
            "type" to enumField(StepEventType.values().map { it.name.lowercase() }, "Event type"),
            "name" to stringField("Optional event name filter"),
            "timeoutMs" to integerField("Wait timeout in ms (default 10000)"),
        ),
        required = listOf("type"),
    ),
    handler = { args ->
        val typeStr = args["type"]?.jsonPrimitive?.content
        val type = typeStr?.let { runCatching { StepEventType.valueOf(it.uppercase()) }.getOrNull() }
        if (type == null) {
            errorResult("missing or invalid 'type' (expected one of: ${StepEventType.values().joinToString { it.name.lowercase() }})")
        } else {
            val name = args["name"]?.jsonPrimitive?.content
            val timeoutMs = args["timeoutMs"]?.jsonPrimitive?.long ?: DEFAULT_TIMEOUT_MS
            stepResultToToolResult(ctx.interpreter.dispatch(Step.WaitForEvent(type, name, timeoutMs)))
        }
    },
)

private fun assertNoEventTool(ctx: ToolContext) = Tool(
    name = "rudder.assert_no_event",
    description = "Watch for windowMs and fail if an event of the given type/name arrives.",
    inputSchema = objectSchema(
        properties = mapOf(
            "type" to enumField(StepEventType.values().map { it.name.lowercase() }, "Event type"),
            "name" to stringField("Optional event name filter"),
            "windowMs" to integerField("Watch window in ms (default 2000)"),
        ),
        required = listOf("type"),
    ),
    handler = { args ->
        val typeStr = args["type"]?.jsonPrimitive?.content
        val type = typeStr?.let { runCatching { StepEventType.valueOf(it.uppercase()) }.getOrNull() }
        if (type == null) {
            errorResult("missing or invalid 'type'")
        } else {
            val name = args["name"]?.jsonPrimitive?.content
            val windowMs = args["windowMs"]?.jsonPrimitive?.long ?: DEFAULT_NO_EVENT_WINDOW_MS
            stepResultToToolResult(ctx.interpreter.dispatch(Step.AssertNoEvent(type, name, windowMs)))
        }
    },
)

private fun assertFieldTool(ctx: ToolContext) = Tool(
    name = "rudder.assert_field",
    description = "Assert that the dotted-path field on the most-recent waited-for event equals the expected JSON value. " +
        "Must be preceded by rudder.assert_next_event in the same session.",
    inputSchema = objectSchema(
        properties = mapOf(
            "path" to stringField("Dotted path into the event JSON (e.g. 'context.sessionId')"),
            "expected" to anyJsonField("Expected JSON value — any primitive (string, number, boolean, null), object, or array."),
        ),
        required = listOf("path", "expected"),
    ),
    handler = { args ->
        val path = args["path"]?.jsonPrimitive?.content
        val expected: JsonElement? = args["expected"]
        if (path == null || expected == null) {
            errorResult("missing required arguments 'path' and 'expected'")
        } else {
            stepResultToToolResult(ctx.interpreter.dispatch(Step.AssertField(path, expected)))
        }
    },
)

private fun waitForBatchTool(ctx: ToolContext) = Tool(
    name = "rudder.wait_for_batch",
    description = "Block until the next batch arrives at the mock plane.",
    inputSchema = objectSchema(
        properties = mapOf("timeoutMs" to integerField("Wait timeout in ms (default 10000)")),
    ),
    handler = { args ->
        val timeoutMs = args["timeoutMs"]?.jsonPrimitive?.long ?: DEFAULT_TIMEOUT_MS
        stepResultToToolResult(ctx.interpreter.dispatch(Step.WaitForBatch(timeoutMs)))
    },
)

/**
 * Read-only debug view onto the wire transcript. Use this when an `assert_next_event` times
 * out or asserts the wrong field — the response shows what the SDK actually emitted, including
 * events that fired before any waiter subscribed. Does not advance the consume cursor.
 */
private fun peekEventsTool(ctx: ToolContext) = Tool(
    name = "rudder.peek_events",
    description = "Return up to `limit` most-recent events from the wire transcript as a JSON array, " +
        "without consuming them. Useful for debugging when an assertion fails or times out — call this " +
        "to see what actually arrived. Does NOT affect the `rudder.assert_next_event` cursor; the same " +
        "events remain available for assertion. Returns `[]` when the transcript is empty.",
    inputSchema = objectSchema(
        properties = mapOf(
            "limit" to integerField("Max events to return (default 20). Returns the tail — most-recent."),
        ),
    ),
    handler = { args ->
        val limit = (args["limit"]?.jsonPrimitive?.long?.toInt() ?: DEFAULT_PEEK_LIMIT)
            .coerceAtLeast(0)
        val events = ctx.mockPlane.peekEvents(limit)
        textResult(Json.encodeToString(JsonArray.serializer(), JsonArray(events)))
    },
)

/** Build an enum-restricted JSON Schema field. v1 supports string enums only — fine for our use. */
private fun enumField(values: List<String>, description: String? = null) = kotlinx.serialization.json.buildJsonObject {
    put("type", JsonPrimitive("string"))
    put("enum", kotlinx.serialization.json.JsonArray(values.map { JsonPrimitive(it) }))
    if (description != null) put("description", JsonPrimitive(description))
}

private const val DEFAULT_TIMEOUT_MS = 10_000L
private const val DEFAULT_NO_EVENT_WINDOW_MS = 2_000L
private const val DEFAULT_PEEK_LIMIT = 20
