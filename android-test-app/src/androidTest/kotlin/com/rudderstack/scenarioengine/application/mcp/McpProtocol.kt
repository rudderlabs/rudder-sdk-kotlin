package com.rudderstack.scenarioengine.application.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Hand-rolled JSON-RPC 2.0 + MCP message shapes for the Step 9 server.
 *
 * **Why not the official MCP Kotlin SDK.** The SDK (`io.modelcontextprotocol:kotlin-sdk:0.12.0`)
 * pulls Kotlin stdlib 2.3.x and Ktor 3.x; this project pins Kotlin 1.9.0 and bumping it is far
 * out of Step 9 scope. The protocol surface we need is small — `initialize`, `tools/list`,
 * `tools/call`, plus `notifications/initialized` — so the cost of authoring it is well under
 * the cost of the version bump.
 *
 * **Spec compatibility.** Mirrors the MCP 2024-11-05 wire format with the legacy SSE transport:
 * the client opens an SSE stream, server pushes responses via `event: message` frames, and the
 * client posts JSON-RPC requests on a separate POST endpoint. That shape is what current
 * MCP clients (Claude Desktop, mcp-inspector) speak when they don't negotiate Streamable HTTP.
 */
internal const val JSON_RPC_VERSION = "2.0"
internal const val MCP_PROTOCOL_VERSION = "2024-11-05"
internal const val MCP_SERVER_NAME = "rudder-scenario-engine"
internal const val MCP_SERVER_VERSION = "0.1.0"

/**
 * One JSON-RPC frame coming in on `POST /message`. The `id` is null for notifications; methods
 * named `notifications/<name>` per MCP convention. `params` is the method-specific payload —
 * we keep it as a [JsonElement] and decode on demand so the dispatcher doesn't have to know
 * every method's shape at compile time.
 */
@Serializable
internal data class JsonRpcRequest(
    val jsonrpc: String = JSON_RPC_VERSION,
    val id: JsonElement? = null,
    val method: String,
    val params: JsonElement? = null,
)

/**
 * Server response to a [JsonRpcRequest]. Exactly one of [result] / [error] is non-null.
 * Notification (`id == null`) requests get no response — the [McpServer] dispatcher returns
 * null to the transport in that case and nothing is written to the SSE stream.
 */
@Serializable
internal data class JsonRpcResponse(
    val jsonrpc: String = JSON_RPC_VERSION,
    val id: JsonElement? = null,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null,
)

/** Standard JSON-RPC error envelope. Codes follow the JSON-RPC 2.0 spec. */
@Serializable
internal data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null,
) {
    companion object {
        const val PARSE_ERROR = -32700
        const val INVALID_REQUEST = -32600
        const val METHOD_NOT_FOUND = -32601
        const val INVALID_PARAMS = -32602
        const val INTERNAL_ERROR = -32603
    }
}

/** `initialize` request params. We accept whatever the client sends and only echo what's relevant. */
@Serializable
internal data class InitializeParams(
    val protocolVersion: String = MCP_PROTOCOL_VERSION,
    val capabilities: JsonObject = JsonObject(emptyMap()),
    val clientInfo: ClientInfo? = null,
)

@Serializable
internal data class ClientInfo(val name: String, val version: String)

/**
 * `initialize` response. Announces only the `tools` capability — no prompts, no resources,
 * no completions in v1. `listChanged = false` because the tool surface is fixed at server
 * construction time; we don't dynamically register tools mid-session.
 */
@Serializable
internal data class InitializeResult(
    val protocolVersion: String = MCP_PROTOCOL_VERSION,
    val capabilities: ServerCapabilities = ServerCapabilities(),
    val serverInfo: ServerInfo = ServerInfo(MCP_SERVER_NAME, MCP_SERVER_VERSION),
    val instructions: String? = null,
)

@Serializable
internal data class ServerCapabilities(
    val tools: ToolsCapability = ToolsCapability(),
)

@Serializable
internal data class ToolsCapability(
    val listChanged: Boolean = false,
)

@Serializable
internal data class ServerInfo(val name: String, val version: String)

/** `tools/list` response. */
@Serializable
internal data class ToolsListResult(val tools: List<ToolInfo>)

/**
 * One entry in the tool catalog returned by `tools/list`. The `inputSchema` is a JSON Schema
 * (Draft 7) object describing the tool's parameters — clients use this for input validation
 * and prompt generation. We hand-author each tool's schema in the per-category tool files;
 * no runtime derivation.
 */
@Serializable
internal data class ToolInfo(
    val name: String,
    val description: String,
    val inputSchema: JsonObject,
)

/** `tools/call` request params. `arguments` is the tool's input — caller-provided JSON. */
@Serializable
internal data class ToolCallParams(
    val name: String,
    val arguments: JsonObject = JsonObject(emptyMap()),
)

/**
 * `tools/call` response. Tool output flows in `content` as text blocks; we serialize the
 * engine's [com.rudderstack.scenarioengine.domain.scenario.StepResult] into JSON and return
 * it as a single text block. `isError = true` flips on engine-side failure, leaving the
 * content carrying the failure reason for the AI to read.
 */
@Serializable
internal data class ToolCallResult(
    val content: List<ToolContent>,
    val isError: Boolean = false,
)

@Serializable
internal data class ToolContent(
    val type: String = "text",
    val text: String,
)
