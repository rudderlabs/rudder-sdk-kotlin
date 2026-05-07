package com.rudderstack.scenarioengine.application.mcp.tools

import com.rudderstack.scenarioengine.application.mcp.Tool
import com.rudderstack.scenarioengine.application.mcp.ToolContext
import com.rudderstack.scenarioengine.application.mcp.ToolRegistry
import com.rudderstack.scenarioengine.application.mcp.errorResult
import com.rudderstack.scenarioengine.application.mcp.stepResultToToolResult
import com.rudderstack.scenarioengine.domain.step.Step
import kotlinx.serialization.json.jsonPrimitive

/**
 * Two spy-plugin tools wired to [Step.AddSpyPlugin] / [Step.RemoveSpyPlugin]. The driver-side
 * [com.rudderstack.scenarioengine.domain.helper.SpyOracle] receives observations on its own
 * broadcast channel — there's no "read observations" tool here because at v1 the AI doesn't
 * need to introspect the spy stream live; the existing assertion tools cover the read shape
 * the AI cares about.
 */
internal fun registerSpyTools(registry: ToolRegistry, ctx: ToolContext) {
    registry.register(
        Tool(
            name = "rudder.add_spy_plugin",
            description = "Register a SUT-side SpyPlugin under a tag. Subsequent SDK events will be observed.",
            inputSchema = objectSchema(
                properties = mapOf("tag" to stringField("String tag to register the spy under")),
                required = listOf("tag"),
            ),
            handler = { args ->
                val tag = args["tag"]?.jsonPrimitive?.content
                if (tag == null) {
                    errorResult("missing required argument 'tag'")
                } else {
                    stepResultToToolResult(ctx.interpreter.dispatch(Step.AddSpyPlugin(tag)))
                }
            },
        ),
    )
    registry.register(
        Tool(
            name = "rudder.remove_spy_plugin",
            description = "Remove a previously-added SpyPlugin by tag.",
            inputSchema = objectSchema(
                properties = mapOf("tag" to stringField("Tag the spy was registered under")),
                required = listOf("tag"),
            ),
            handler = { args ->
                val tag = args["tag"]?.jsonPrimitive?.content
                if (tag == null) {
                    errorResult("missing required argument 'tag'")
                } else {
                    stepResultToToolResult(ctx.interpreter.dispatch(Step.RemoveSpyPlugin(tag)))
                }
            },
        ),
    )
}
