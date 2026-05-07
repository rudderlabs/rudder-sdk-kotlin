package com.rudderstack.scenarioengine.application.mcp

import com.rudderstack.scenarioengine.application.interpreter.Helpers
import com.rudderstack.scenarioengine.application.interpreter.SequentialInterpreter
import com.rudderstack.scenarioengine.application.mcp.tools.registerAssertionTools
import com.rudderstack.scenarioengine.application.mcp.tools.registerDiscoveryTools
import com.rudderstack.scenarioengine.application.mcp.tools.registerEventTools
import com.rudderstack.scenarioengine.application.mcp.tools.registerLifecycleTools
import com.rudderstack.scenarioengine.application.mcp.tools.registerSessionTools
import com.rudderstack.scenarioengine.application.mcp.tools.registerSpyTools
import com.rudderstack.scenarioengine.application.mcp.tools.registerStateTools
import com.rudderstack.scenarioengine.application.mcp.transport.HttpSseTransport
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Top-level MCP server. Wires the [HttpSseTransport] to the JSON-RPC dispatcher and the
 * [ToolRegistry], registers every tool wired in v1, exposes [start] / [stop] / [localUrl].
 *
 * **Lifecycle.** One server per live-mode `@Test`. The test's `@Before` builds the engine
 * helpers + interpreter (via [com.rudderstack.scenarioengine.scenarios.ScenarioRunnerTest]),
 * the `@Test` constructs an [McpServer], starts it, and blocks. `@After` shuts the server down.
 *
 * **Why register tools in groups.** Each `tools/<Group>Tools.kt` registers the tools its
 * file owns. Splitting registration along the same boundary keeps adding a new tool to a
 * one-file change and keeps this orchestrator unchanged.
 */
internal class McpServer(
    helpers: Helpers,
    interpreter: SequentialInterpreter,
    private val port: Int = DEFAULT_PORT,
) {
    private val registry = ToolRegistry()

    private val transport = HttpSseTransport(
        port = port,
        requestHandler = ::handleRequest,
    )

    private val json = Json {
        // Keep the wire shape forgiving: tolerate fields the spec adds in future minor
        // versions (clientInfo extras, capabilities subkeys) without failing the request.
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    init {
        val ctx = ToolContext(
            interpreter = interpreter,
            mockPlane = helpers.mockPlane,
            state = helpers.state,
            spy = helpers.spy,
            mockPlaneUrl = helpers.mockPlane.baseUrl,
        )
        // Order is registration order — clients display tools in this order. Group by
        // category for legibility; doc-named ordering would be alphabetical, which buries
        // related ops.
        registerStateTools(registry, ctx)
        registerEventTools(registry, ctx)
        registerSessionTools(registry, ctx)
        registerLifecycleTools(registry, ctx)
        registerAssertionTools(registry, ctx)
        registerSpyTools(registry, ctx)
        registerDiscoveryTools(registry)
    }

    fun start() {
        transport.start()
    }

    fun stop() {
        transport.stop()
    }

    /** Local URL clients connect to. Matches the URL the `startMcpLive` Gradle task `adb reverse`s. */
    val localUrl: String get() = transport.localUrl

    /**
     * Decode an inbound JSON-RPC frame, dispatch by method, encode the response. Returns null
     * for notifications (id == null) so the transport doesn't push an empty frame down the
     * SSE stream. Errors at this level (parse, unknown method, handler exception) are caught
     * and turned into JSON-RPC error responses — the only `null` return is for valid
     * notifications.
     */
    private suspend fun handleRequest(rawBody: String): String? {
        val request = runCatching { json.decodeFromString(JsonRpcRequest.serializer(), rawBody) }
            .getOrElse { return jsonRpcError(null, JsonRpcError.PARSE_ERROR, "parse error: ${it.message}") }

        if (request.id == null && request.method.startsWith("notifications/")) {
            handleNotification(request)
            return null
        }

        return runCatching { dispatch(request) }
            .getOrElse {
                jsonRpcError(
                    request.id,
                    JsonRpcError.INTERNAL_ERROR,
                    "${it::class.simpleName}: ${it.message ?: "unknown"}",
                )
            }
    }

    private suspend fun dispatch(request: JsonRpcRequest): String = when (request.method) {
        "initialize" -> {
            val result = json.encodeToJsonElement(InitializeResult.serializer(), InitializeResult())
            jsonRpcOk(request.id, result)
        }
        "tools/list" -> {
            val result = json.encodeToJsonElement(
                ToolsListResult.serializer(),
                ToolsListResult(tools = registry.list()),
            )
            jsonRpcOk(request.id, result)
        }
        "tools/call" -> {
            val params = decodeParamsOrFail(request, ToolCallParams.serializer())
            if (params == null) {
                jsonRpcError(request.id, JsonRpcError.INVALID_PARAMS, "invalid params for tools/call")
            } else {
                val callResult = registry.call(params.name, params.arguments)
                val result = json.encodeToJsonElement(ToolCallResult.serializer(), callResult)
                jsonRpcOk(request.id, result)
            }
        }
        else -> jsonRpcError(request.id, JsonRpcError.METHOD_NOT_FOUND, "method not found: ${request.method}")
    }

    /**
     * MCP notifications we accept. `notifications/initialized` is the post-handshake ping —
     * no-op on the server side. Future notifications (e.g. `notifications/cancelled`) plug
     * in here without touching the request dispatcher.
     */
    private fun handleNotification(request: JsonRpcRequest) {
        when (request.method) {
            "notifications/initialized" -> Unit
            else -> Unit // Silently ignore unknown notifications, per the JSON-RPC convention.
        }
    }

    private inline fun <reified T> decodeParamsOrFail(
        request: JsonRpcRequest,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): T? {
        val params = request.params ?: return null
        return try {
            json.decodeFromJsonElement(serializer, params)
        } catch (_: SerializationException) {
            null
        }
    }

    private fun jsonRpcOk(id: JsonElement?, result: JsonElement): String =
        json.encodeToString(JsonRpcResponse.serializer(), JsonRpcResponse(id = id ?: JsonNull, result = result))

    private fun jsonRpcError(id: JsonElement?, code: Int, message: String): String =
        json.encodeToString(
            JsonRpcResponse.serializer(),
            JsonRpcResponse(id = id ?: JsonNull, error = JsonRpcError(code = code, message = message)),
        )

    private companion object {
        const val DEFAULT_PORT = 5111
    }
}
