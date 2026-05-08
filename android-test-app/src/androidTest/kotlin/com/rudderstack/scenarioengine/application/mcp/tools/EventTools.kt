package com.rudderstack.scenarioengine.application.mcp.tools

import com.rudderstack.scenarioengine.application.mcp.Tool
import com.rudderstack.scenarioengine.application.mcp.ToolContext
import com.rudderstack.scenarioengine.application.mcp.ToolRegistry
import com.rudderstack.scenarioengine.application.mcp.errorResult
import com.rudderstack.scenarioengine.application.mcp.stepResultToToolResult
import com.rudderstack.scenarioengine.domain.step.Step
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Eight event-flavored tools — one per public event method on the SDK. Each handler builds a
 * single [Step] from the tool's JSON arguments and pushes it through the interpreter's
 * per-step dispatcher.
 *
 * Mirrors the Step taxonomy 1:1 (see §4.1): track / screen / identify / group / alias / flush /
 * reset / shutdown. Tool *names* follow the doc's `rudder.<verb>` convention (§14.4).
 */
internal fun registerEventTools(registry: ToolRegistry, ctx: ToolContext) {
    registry.register(trackTool(ctx))
    registry.register(screenTool(ctx))
    registry.register(identifyTool(ctx))
    registry.register(groupTool(ctx))
    registry.register(aliasTool(ctx))
    registry.register(flushTool(ctx))
    registry.register(resetTool(ctx))
    registry.register(shutdownTool(ctx))
}

private fun trackTool(ctx: ToolContext) = Tool(
    name = "rudder.track",
    description = "Send a Track event with the given name and optional properties.",
    inputSchema = objectSchema(
        properties = mapOf(
            "name" to stringField("Event name"),
            "properties" to objectField("Event properties (free-form JSON object)"),
        ),
        required = listOf("name"),
    ),
    handler = { args ->
        val name = args["name"]?.jsonPrimitive?.content
        if (name == null) {
            errorResult("missing required argument 'name'")
        } else {
            val properties = args["properties"]?.jsonObject ?: JsonObject(emptyMap())
            stepResultToToolResult(ctx.interpreter.dispatch(Step.Track(name, properties)))
        }
    },
)

private fun screenTool(ctx: ToolContext) = Tool(
    name = "rudder.screen",
    description = "Send a Screen event.",
    inputSchema = objectSchema(
        properties = mapOf(
            "name" to stringField("Screen name"),
            "category" to stringField("Optional screen category"),
            "properties" to objectField("Optional screen properties"),
        ),
        required = listOf("name"),
    ),
    handler = { args ->
        val name = args["name"]?.jsonPrimitive?.content
        if (name == null) {
            errorResult("missing required argument 'name'")
        } else {
            val category = args["category"]?.jsonPrimitive?.content
            val properties = args["properties"]?.jsonObject ?: JsonObject(emptyMap())
            stepResultToToolResult(ctx.interpreter.dispatch(Step.Screen(name, category, properties)))
        }
    },
)

private fun identifyTool(ctx: ToolContext) = Tool(
    name = "rudder.identify",
    description = "Identify the current user with a userId and optional traits.",
    inputSchema = objectSchema(
        properties = mapOf(
            "userId" to stringField("User identifier"),
            "traits" to objectField("Optional identity traits"),
        ),
        required = listOf("userId"),
    ),
    handler = { args ->
        val userId = args["userId"]?.jsonPrimitive?.content
        if (userId == null) {
            errorResult("missing required argument 'userId'")
        } else {
            val traits = args["traits"]?.jsonObject ?: JsonObject(emptyMap())
            stepResultToToolResult(ctx.interpreter.dispatch(Step.Identify(userId, traits)))
        }
    },
)

private fun groupTool(ctx: ToolContext) = Tool(
    name = "rudder.group",
    description = "Associate the current user with a group.",
    inputSchema = objectSchema(
        properties = mapOf(
            "groupId" to stringField("Group identifier"),
            "traits" to objectField("Optional group traits"),
        ),
        required = listOf("groupId"),
    ),
    handler = { args ->
        val groupId = args["groupId"]?.jsonPrimitive?.content
        if (groupId == null) {
            errorResult("missing required argument 'groupId'")
        } else {
            val traits = args["traits"]?.jsonObject ?: JsonObject(emptyMap())
            stepResultToToolResult(ctx.interpreter.dispatch(Step.Group(groupId, traits)))
        }
    },
)

private fun aliasTool(ctx: ToolContext) = Tool(
    name = "rudder.alias",
    description = "Merge two identities under a single user. previousId defaults to the current userId.",
    inputSchema = objectSchema(
        properties = mapOf(
            "newId" to stringField("New user id"),
            "previousId" to stringField("Optional previous id; SDK auto-resolves if omitted"),
        ),
        required = listOf("newId"),
    ),
    handler = { args ->
        val newId = args["newId"]?.jsonPrimitive?.content
        if (newId == null) {
            errorResult("missing required argument 'newId'")
        } else {
            val previousId = args["previousId"]?.jsonPrimitive?.content
            stepResultToToolResult(ctx.interpreter.dispatch(Step.Alias(newId, previousId)))
        }
    },
)

private fun flushTool(ctx: ToolContext) = Tool(
    name = "rudder.flush",
    description = "Force the SDK to drain its queue to the data plane immediately.",
    inputSchema = objectSchema(properties = emptyMap()),
    handler = { stepResultToToolResult(ctx.interpreter.dispatch(Step.Flush)) },
)

private fun resetTool(ctx: ToolContext) = Tool(
    name = "rudder.reset",
    description = "Reset some or all of the SDK's identity state. All four flags default to true (full reset).",
    inputSchema = objectSchema(
        properties = mapOf(
            "anonymousId" to booleanField("Regenerate anonymousId (default true)"),
            "userId" to booleanField("Clear userId (default true)"),
            "traits" to booleanField("Clear traits (default true)"),
            "session" to booleanField("Refresh session — note: this *rotates*, not clears (default true)"),
        ),
    ),
    handler = { args ->
        val anonymousId = args["anonymousId"]?.jsonPrimitive?.boolean ?: true
        val userId = args["userId"]?.jsonPrimitive?.boolean ?: true
        val traits = args["traits"]?.jsonPrimitive?.boolean ?: true
        val session = args["session"]?.jsonPrimitive?.boolean ?: true
        stepResultToToolResult(
            ctx.interpreter.dispatch(Step.Reset(anonymousId, userId, traits, session)),
        )
    },
)

private fun shutdownTool(ctx: ToolContext) = Tool(
    name = "rudder.shutdown",
    description = "Tear down the SDK instance. Subsequent event tools no-op until rudder.init is called again.",
    inputSchema = objectSchema(properties = emptyMap()),
    handler = { stepResultToToolResult(ctx.interpreter.dispatch(Step.Shutdown)) },
)

// --- shared schema helpers ---

internal fun objectSchema(
    properties: Map<String, JsonObject>,
    required: List<String> = emptyList(),
): JsonObject = kotlinx.serialization.json.buildJsonObject {
    put("type", JsonPrimitive("object"))
    put("properties", JsonObject(properties))
    if (required.isNotEmpty()) {
        put("required", kotlinx.serialization.json.JsonArray(required.map { JsonPrimitive(it) }))
    }
}

internal fun stringField(description: String? = null): JsonObject = kotlinx.serialization.json.buildJsonObject {
    put("type", JsonPrimitive("string"))
    if (description != null) put("description", JsonPrimitive(description))
}

internal fun booleanField(description: String? = null): JsonObject = kotlinx.serialization.json.buildJsonObject {
    put("type", JsonPrimitive("boolean"))
    if (description != null) put("description", JsonPrimitive(description))
}

internal fun objectField(description: String? = null): JsonObject = kotlinx.serialization.json.buildJsonObject {
    put("type", JsonPrimitive("object"))
    if (description != null) put("description", JsonPrimitive(description))
}

/**
 * "Any JSON value" schema — primitive, object, or array. Omitting `type` is the JSON Schema
 * idiom for unconstrained values; `objectField` (which pins `type: object`) lies for callers
 * that want to pass a bare `false` or `"foo"`.
 */
internal fun anyJsonField(description: String? = null): JsonObject = kotlinx.serialization.json.buildJsonObject {
    if (description != null) put("description", JsonPrimitive(description))
}

internal fun integerField(description: String? = null): JsonObject = kotlinx.serialization.json.buildJsonObject {
    put("type", JsonPrimitive("integer"))
    if (description != null) put("description", JsonPrimitive(description))
}
