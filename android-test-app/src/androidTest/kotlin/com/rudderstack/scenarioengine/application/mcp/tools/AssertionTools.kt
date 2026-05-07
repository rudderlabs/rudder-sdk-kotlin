package com.rudderstack.scenarioengine.application.mcp.tools

import com.rudderstack.scenarioengine.application.mcp.Tool
import com.rudderstack.scenarioengine.application.mcp.ToolContext
import com.rudderstack.scenarioengine.application.mcp.ToolRegistry
import com.rudderstack.scenarioengine.application.mcp.errorResult
import com.rudderstack.scenarioengine.application.mcp.stepResultToToolResult
import com.rudderstack.scenarioengine.domain.step.Step
import com.rudderstack.scenarioengine.domain.step.StepEventType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * Four assertion tools — the shape of the wire / event-cache oracles the AI uses to verify
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
 */
internal fun registerAssertionTools(registry: ToolRegistry, ctx: ToolContext) {
    registry.register(assertNextEventTool(ctx))
    registry.register(assertNoEventTool(ctx))
    registry.register(assertFieldTool(ctx))
    registry.register(waitForBatchTool(ctx))
}

private fun assertNextEventTool(ctx: ToolContext) = Tool(
    name = "rudder.assert_next_event",
    description = "Wait for the next event matching type/name to appear on the mock plane and return it. " +
        "The event becomes the target for a subsequent rudder.assert_field call.",
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
            "expected" to objectField("Expected JSON value (any primitive / object / array)"),
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

/** Build an enum-restricted JSON Schema field. v1 supports string enums only — fine for our use. */
private fun enumField(values: List<String>, description: String? = null) = kotlinx.serialization.json.buildJsonObject {
    put("type", JsonPrimitive("string"))
    put("enum", kotlinx.serialization.json.JsonArray(values.map { JsonPrimitive(it) }))
    if (description != null) put("description", JsonPrimitive(description))
}

private const val DEFAULT_TIMEOUT_MS = 10_000L
private const val DEFAULT_NO_EVENT_WINDOW_MS = 2_000L
