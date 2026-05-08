package com.rudderstack.scenarioengine.application.mcp.tools

import com.rudderstack.scenarioengine.application.mcp.Tool
import com.rudderstack.scenarioengine.application.mcp.ToolContext
import com.rudderstack.scenarioengine.application.mcp.ToolRegistry
import com.rudderstack.scenarioengine.application.mcp.errorResult
import com.rudderstack.scenarioengine.application.mcp.stepResultToToolResult
import com.rudderstack.scenarioengine.application.mcp.textResult
import com.rudderstack.scenarioengine.domain.step.StateField
import com.rudderstack.scenarioengine.domain.step.Step
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * Two state-flavored tools:
 *
 *  - `rudder.init` — the SDK lifecycle's entry point. Builds a [Step.Init] with the live mock
 *    server URL pre-filled from [ToolContext.mockPlaneUrl] so the AI doesn't have to know the
 *    loopback address. The writeKey defaults to the engine's standard test key; tests can
 *    override but rarely need to.
 *  - `rudder.read_state` — direct read from the [com.rudderstack.scenarioengine.domain.helper.StateProbe],
 *    *not* a Step dispatch. The doc's [Step.AssertState] checks for equality against an
 *    expected value; an MCP-side AI agent wants to *retrieve* the value before deciding what
 *    to assert, so this tool returns the probed value as the response text.
 */
internal fun registerStateTools(registry: ToolRegistry, ctx: ToolContext) {
    registry.register(initTool(ctx))
    registry.register(readStateTool(ctx))
}

private fun initTool(ctx: ToolContext) = Tool(
    name = "rudder.init",
    description = "Initialize the SDK with the engine's live mock-server URL. Must be called once " +
        "at the start of an MCP session before any event/lifecycle/assertion tools. " +
        "**IMPORTANT — opt into the SDK behaviors you want to test:** " +
        "to verify `Application Opened` / `Application Backgrounded` Track events on " +
        "background/foreground, pass `trackApplicationLifecycleEvents = true`. " +
        "To verify auto-session rotation across long backgrounds, pass " +
        "`automaticSessionTracking = true` (and a `sessionTimeoutMs`). " +
        "These flags map 1:1 to the SDK's `Configuration` knobs and are off by default; " +
        "asserting on lifecycle/session events without enabling the matching flag will " +
        "time out because the SDK never emits them.",
    inputSchema = objectSchema(
        properties = mapOf(
            "writeKey" to stringField("Optional write key (default: 'test-write-key')"),
            "trackApplicationLifecycleEvents" to booleanField(
                "Default false. **Set true to test lifecycle events.** When true, the SDK " +
                    "emits `Application Opened` (with `properties.from_background = false` on " +
                    "the first onStart, then `true` on subsequent foregroundings) and " +
                    "`Application Backgrounded` Track events as the process moves between " +
                    "foreground and background."
            ),
            "trackDeepLinks" to booleanField("Default false. Set true to test `Deep Link Opened` events."),
            "trackActivities" to booleanField("Default false. Set true to test per-Activity screen events."),
            "automaticSessionTracking" to booleanField(
                "Default false. **Set true to test session rotation.** When true, the SDK " +
                    "auto-starts a session on init and rotates it after a background longer " +
                    "than `sessionTimeoutMs`. Without this flag, sessions are only managed " +
                    "via `rudder.start_session` / `rudder.end_session`."
            ),
            "sessionTimeoutMs" to integerField(
                "Optional auto-session timeout in ms. Only meaningful with " +
                    "`automaticSessionTracking = true`."
            ),
            "flushAt" to integerField(
                "Override flushPolicies to a single CountFlushPolicy(N). Default 1 — every " +
                    "event auto-flushes immediately, which is what most assertion flows want. " +
                    "Use a high value (e.g. 100) when explicitly testing the queue / `flush()` " +
                    "behavior."
            ),
        ),
    ),
    handler = { args ->
        val writeKey = args["writeKey"]?.jsonPrimitive?.content ?: DEFAULT_WRITE_KEY
        val trackLifecycle = args["trackApplicationLifecycleEvents"]?.jsonPrimitive?.boolean ?: false
        val trackDeepLinks = args["trackDeepLinks"]?.jsonPrimitive?.boolean ?: false
        val trackActivities = args["trackActivities"]?.jsonPrimitive?.boolean ?: false
        val automaticSession = args["automaticSessionTracking"]?.jsonPrimitive?.boolean ?: false
        val sessionTimeoutMs = args["sessionTimeoutMs"]?.jsonPrimitive?.long
        val flushAt = args["flushAt"]?.jsonPrimitive?.long?.toInt() ?: 1

        val step = Step.Init(
            writeKey = writeKey,
            mockServerUrl = ctx.mockPlaneUrl,
            trackApplicationLifecycleEvents = trackLifecycle,
            trackDeepLinks = trackDeepLinks,
            trackActivities = trackActivities,
            automaticSessionTracking = automaticSession,
            sessionTimeoutMs = sessionTimeoutMs,
            flushAt = flushAt,
        )
        stepResultToToolResult(ctx.interpreter.dispatch(step))
    },
)

private fun readStateTool(ctx: ToolContext) = Tool(
    name = "rudder.read_state",
    description = "Read a piece of in-SDK identity state. Returns the value as text, or 'null'/empty for unset/cleared. " +
        "Note that USER_ID returns the empty string when cleared, while SESSION_ID returns null — see the StateProbe contract.",
    inputSchema = objectSchema(
        properties = mapOf(
            "field" to enumField(StateField.values().map { it.name.lowercase() }, "Which identity field to read"),
        ),
        required = listOf("field"),
    ),
    handler = { args ->
        val fieldStr = args["field"]?.jsonPrimitive?.content
        val field = fieldStr?.let { runCatching { StateField.valueOf(it.uppercase()) }.getOrNull() }
        if (field == null) {
            errorResult("missing or invalid 'field' (expected one of: ${StateField.values().joinToString { it.name.lowercase() }})")
        } else {
            val value = when (field) {
                StateField.ANONYMOUS_ID -> ctx.state.anonymousId()
                StateField.USER_ID -> ctx.state.userId()
                StateField.SESSION_ID -> ctx.state.sessionId()
            }
            textResult(value ?: "null")
        }
    },
)

private fun enumField(values: List<String>, description: String? = null) = kotlinx.serialization.json.buildJsonObject {
    put("type", kotlinx.serialization.json.JsonPrimitive("string"))
    put("enum", kotlinx.serialization.json.JsonArray(values.map { kotlinx.serialization.json.JsonPrimitive(it) }))
    if (description != null) put("description", kotlinx.serialization.json.JsonPrimitive(description))
}

private const val DEFAULT_WRITE_KEY = "test-write-key"
