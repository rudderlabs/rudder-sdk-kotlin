package com.rudderstack.scenarioengine.application.mcp.tools

import com.rudderstack.scenarioengine.application.mcp.Tool
import com.rudderstack.scenarioengine.application.mcp.ToolContext
import com.rudderstack.scenarioengine.application.mcp.ToolRegistry
import com.rudderstack.scenarioengine.application.mcp.stepResultToToolResult
import com.rudderstack.scenarioengine.domain.step.Step
import kotlinx.serialization.json.long
import kotlinx.serialization.json.jsonPrimitive

/**
 * Two session tools mirroring [Step.StartSession] / [Step.EndSession]. `start_session` accepts
 * an optional sessionId — if supplied, the SDK runs in manual-session mode with that id;
 * otherwise the SDK auto-assigns. The SDK rejects ids shorter than 10 digits silently, so
 * the tool description warns explicitly.
 */
internal fun registerSessionTools(registry: ToolRegistry, ctx: ToolContext) {
    registry.register(
        Tool(
            name = "rudder.start_session",
            description = "Start a session. If sessionId is omitted, the SDK auto-assigns. " +
                "Manual ids must be at least 10 digits — shorter ids are silently rejected by the SDK.",
            inputSchema = objectSchema(
                properties = mapOf("sessionId" to integerField("Optional manual session id (>= 10 digits)")),
            ),
            handler = { args ->
                val sessionId = args["sessionId"]?.jsonPrimitive?.long
                stepResultToToolResult(ctx.interpreter.dispatch(Step.StartSession(sessionId)))
            },
        ),
    )
    registry.register(
        Tool(
            name = "rudder.end_session",
            description = "End the current session.",
            inputSchema = objectSchema(properties = emptyMap()),
            handler = { stepResultToToolResult(ctx.interpreter.dispatch(Step.EndSession)) },
        ),
    )
}
