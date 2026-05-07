package com.rudderstack.scenarioengine.application.mcp.tools

import com.rudderstack.scenarioengine.application.mcp.Tool
import com.rudderstack.scenarioengine.application.mcp.ToolContext
import com.rudderstack.scenarioengine.application.mcp.ToolRegistry
import com.rudderstack.scenarioengine.application.mcp.stepResultToToolResult
import com.rudderstack.scenarioengine.domain.step.Step

/**
 * Six lifecycle tools — direct 1:1 mapping to the SUT-process lifecycle Steps wired in 6a/6b.
 * No arguments on any of them; each is a verb that the engine's lifecycle adapter executes
 * via `am`/`pm` shell calls.
 *
 * **Why no `am crash` / `pm grant` here.** The doc's §14.4 lists `crash` / `clear_app_data`
 * under different categories; clear is wired (it's a destructive lifecycle op), `crash` is a
 * fault Step that's `TODO()` in the interpreter (Step 12 territory) so it isn't exposed yet.
 */
internal fun registerLifecycleTools(registry: ToolRegistry, ctx: ToolContext) {
    registry.register(simpleTool("rudder.background", "Move the SUT to background (HOME key).", ctx, Step.Background))
    registry.register(simpleTool("rudder.foreground", "Foreground the SUT (start its main activity).", ctx, Step.Foreground))
    registry.register(simpleTool("rudder.kill", "Kill the SUT process via `am kill`.", ctx, Step.Kill))
    registry.register(simpleTool("rudder.force_stop", "Force-stop the SUT via `am force-stop`.", ctx, Step.ForceStop))
    registry.register(simpleTool("rudder.cold_start", "Force-stop then re-launch the SUT (full cold start).", ctx, Step.ColdStart))
    registry.register(simpleTool("rudder.clear_app_data", "Wipe the SUT's app data via `pm clear`.", ctx, Step.ClearAppData))
}

/**
 * Build a no-argument tool that dispatches a fixed [Step] singleton. Six lifecycle tools share
 * the exact same shape — extracting the wrapper keeps each registration to a single line of
 * intent ("name + description + which step").
 */
private fun simpleTool(name: String, description: String, ctx: ToolContext, step: Step) = Tool(
    name = name,
    description = description,
    inputSchema = objectSchema(properties = emptyMap()),
    handler = { stepResultToToolResult(ctx.interpreter.dispatch(step)) },
)
