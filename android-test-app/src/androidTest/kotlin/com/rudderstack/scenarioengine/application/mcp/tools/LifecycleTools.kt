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
    registry.register(simpleTool(
        name = "rudder.background",
        description = "Move the SUT to background (HOME key). To observe an `Application Backgrounded` " +
            "Track event on the wire, the preceding `rudder.init` must set " +
            "`trackApplicationLifecycleEvents = true` — otherwise this only moves the process " +
            "without any SDK-emitted event.",
        ctx = ctx,
        step = Step.Background,
    ))
    registry.register(simpleTool(
        name = "rudder.foreground",
        description = "Foreground the SUT (start its main activity). To observe an `Application Opened` " +
            "Track event on the wire, the preceding `rudder.init` must set " +
            "`trackApplicationLifecycleEvents = true`. The first such event after init carries " +
            "`properties.from_background = false`; subsequent foregrounds carry `true`.",
        ctx = ctx,
        step = Step.Foreground,
    ))
    registry.register(simpleTool(
        name = "rudder.kill",
        description = "Kill the SUT process via `kill -9` (run-as). The SDK process dies; on-disk state " +
            "(identity, queued events) is preserved. Pair with `rudder.cold_start` to verify " +
            "what the SDK rehydrates on the new process.",
        ctx = ctx,
        step = Step.Kill,
    ))
    registry.register(simpleTool(
        name = "rudder.force_stop",
        description = "Force-stop the SUT via `am force-stop`. Softer than kill (no SIGKILL). On-disk " +
            "state is preserved; pair with `rudder.cold_start` to verify rehydration.",
        ctx = ctx,
        step = Step.ForceStop,
    ))
    registry.register(simpleTool(
        name = "rudder.cold_start",
        description = "Force-stop then re-launch the SUT (full cold start). After this returns, the " +
            "SUT is alive on a fresh process; you must call `rudder.init` again to bring the " +
            "SDK up before any further event tools.",
        ctx = ctx,
        step = Step.ColdStart,
    ))
    registry.register(simpleTool(
        name = "rudder.clear_app_data",
        description = "Wipe the SUT's app data via `pm clear`. Removes all SDK-persisted state " +
            "(identity, queued events). The SUT process is taken down; you must call " +
            "`rudder.init` again afterward.",
        ctx = ctx,
        step = Step.ClearAppData,
    ))
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
