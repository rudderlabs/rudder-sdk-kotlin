package com.rudderstack.scenarioengine.application.mcp

import com.rudderstack.scenarioengine.application.interpreter.SequentialInterpreter
import com.rudderstack.scenarioengine.domain.helper.MockPlane
import com.rudderstack.scenarioengine.domain.helper.SpyOracle
import com.rudderstack.scenarioengine.domain.helper.StateProbe
import com.rudderstack.scenarioengine.domain.scenario.StepResult
import kotlinx.serialization.json.JsonObject

/**
 * Bag of references the tool handlers need. One slot per engine seam they reach into:
 * the interpreter for Step dispatch, the helpers that the interpreter doesn't already cover
 * (state read, mock-server URL discovery for `init`, spy oracle reads).
 *
 * Tools never see Android types or the underlying transports — same dependency rule the
 * engine itself observes. This keeps MCP handlers testable in isolation by passing fakes,
 * even though Step 9 doesn't add a separate MCP unit-test pass.
 *
 * @param interpreter The engine's per-step dispatcher. Each tool call lands on
 *     [SequentialInterpreter.dispatch] so the interpreter's `lastObservedEvent` cache
 *     persists across MCP tool calls in one session — without that, `assert_field` after a
 *     `wait_for_event` from a prior call would have nothing to assert against.
 * @param mockPlaneUrl The live mock-server URL the SUT is initialised against. Tools that
 *     synthesize a [com.rudderstack.scenarioengine.domain.step.Step.Init] read this to fill
 *     `mockServerUrl` so the AI doesn't have to know the loopback address.
 */
internal data class ToolContext(
    val interpreter: SequentialInterpreter,
    val mockPlane: MockPlane,
    val state: StateProbe,
    val spy: SpyOracle,
    val mockPlaneUrl: String,
)

/**
 * Single tool entry — name, description, schema, handler. Handlers are suspending because
 * Step dispatch is suspending (mock-plane oracles, lifecycle ADB calls, state polls).
 *
 * The handler returns a [ToolCallResult] so it can flag failures via `isError = true` rather
 * than throwing — exceptions in tool handlers turn into JSON-RPC `internal_error` responses
 * and lose the engine's structured failure reason. Returning [ToolCallResult] keeps the
 * engine's [StepResult.Failed] reason visible to the AI client.
 */
internal data class Tool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject,
    val handler: suspend (JsonObject) -> ToolCallResult,
)

/**
 * In-memory tool registry. Single mutable map; tool surface is fixed at server-construction
 * time and not changed mid-session. [ToolsCapability.listChanged] is always false on the wire.
 */
internal class ToolRegistry {
    private val tools = linkedMapOf<String, Tool>()

    fun register(tool: Tool) {
        require(tools.put(tool.name, tool) == null) { "Tool '${tool.name}' already registered." }
    }

    /** Catalog as [ToolInfo] for `tools/list`. Order is registration order — clients display in this order. */
    fun list(): List<ToolInfo> =
        tools.values.map { ToolInfo(it.name, it.description, it.inputSchema) }

    /**
     * Invoke the tool by [name]. Unknown tools resolve to a [ToolCallResult] with `isError = true`
     * — we return errors rather than throwing so the SSE response shape stays uniform regardless
     * of where the failure originated (registry vs. handler).
     */
    suspend fun call(name: String, arguments: JsonObject): ToolCallResult {
        val tool = tools[name]
            ?: return errorResult("Tool '$name' not found. Available: ${tools.keys.joinToString()}")
        return tool.handler(arguments)
    }
}

/** Build an `isError = true` [ToolCallResult] carrying [message] as the only text block. */
internal fun errorResult(message: String): ToolCallResult =
    ToolCallResult(content = listOf(ToolContent(text = message)), isError = true)

/** Wrap [text] as a successful single-block [ToolCallResult]. */
internal fun textResult(text: String): ToolCallResult =
    ToolCallResult(content = listOf(ToolContent(text = text)), isError = false)

/**
 * Serialize a [StepResult] back to the AI as a text block. Failures become `isError` results
 * so the AI sees the structured reason; successes carry the `value` field's `toString` for
 * the few Steps that return a payload (event JSON for `wait_for_event`, batch list for
 * `wait_for_batch`). The string isn't always valid JSON — kotlinx serialization of arbitrary
 * `Any?` is non-trivial; the `toString` covers our v1 needs and an explicit `value_json` field
 * can be added later if a tool's payload needs structured access AI-side.
 */
internal fun stepResultToToolResult(result: StepResult): ToolCallResult = when (result) {
    is StepResult.Ok -> textResult(
        if (result.value == null) "ok" else "ok: ${result.value}",
    )
    is StepResult.Failed -> errorResult("failed: ${result.reason}")
    is StepResult.Skipped -> textResult("skipped: ${result.reason}")
}
