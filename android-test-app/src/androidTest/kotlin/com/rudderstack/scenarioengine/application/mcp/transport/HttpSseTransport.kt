package com.rudderstack.scenarioengine.application.mcp.transport

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * HTTP/SSE transport for the MCP server. Ktor 2.3.13 server with two routes:
 *
 *  - `GET /sse` — opens an SSE stream. The server writes the `endpoint` event first (telling
 *    the client where to POST messages), then loops forwarding queued responses as
 *    `event: message` frames.
 *  - `POST /message?sessionId=<UUID>` — accepts a JSON-RPC request body, hands it to the
 *    [requestHandler], queues the response on the matching session's channel, and returns
 *    202 Accepted immediately. The actual response body lands on the SSE stream, not in the
 *    POST response.
 *
 * **Why legacy SSE, not Streamable HTTP.** Streamable HTTP is the newer (2025-03) MCP transport
 * but is not yet uniformly implemented across MCP clients. Legacy SSE is what mcp-inspector,
 * older Claude Desktop builds, and most curl-based debugging speak. We can add Streamable HTTP
 * later as a sibling route — the [requestHandler] is transport-neutral.
 *
 * **Single-process scope.** The server is per [com.rudderstack.scenarioengine.application.mcp.McpServer]
 * instance, which itself is per live-mode test. Multiple concurrent SSE clients are accepted
 * but share the same engine state; that's the right model for a developer-driven session.
 */
internal class HttpSseTransport(
    private val host: String = DEFAULT_HOST,
    private val port: Int = DEFAULT_PORT,
    private val requestHandler: suspend (String) -> String?,
) {
    /**
     * Active SSE sessions, keyed by the UUID we hand the client in the initial `endpoint` event.
     * Each value is the channel the SSE-writer coroutine drains; POST handlers push responses
     * onto it. ConcurrentHashMap because adds (GET) and removes (disconnect) race with reads
     * (POST), even in a single-client deployment.
     */
    private val sessions = ConcurrentHashMap<String, Channel<String>>()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var server: io.ktor.server.engine.ApplicationEngine? = null

    /** Boot the Ktor engine and bind. Idempotent only in the trivial sense — call once. */
    fun start() {
        server = embeddedServer(CIO, host = host, port = port) {
            configure()
        }.start(wait = false)
    }

    /**
     * Stop the Ktor engine and tear down all sessions. Closing each session's channel is what
     * unblocks the SSE-writer coroutines; without that they would hold the server open past
     * `stop`.
     */
    fun stop() {
        sessions.values.forEach { runCatching { it.close() } }
        sessions.clear()
        runCatching { server?.stop(GRACE_MS, GRACE_MS) }
        runCatching { scope.cancel() }
    }

    /** The local URL clients connect to. Useful for the Gradle task to print after `adb reverse`. */
    val localUrl: String get() = "http://$host:$port/sse"

    private fun Application.configure() {
        // No CORS plugin — this server only ever serves a local-loopback developer client
        // reaching it via `adb reverse`. Browser-driven cross-origin calls aren't a use case;
        // skipping the plugin keeps the dep tree to ktor-server-core + ktor-server-cio.
        routing {
            get("/sse") {
                val sessionId = UUID.randomUUID().toString()
                val channel = Channel<String>(Channel.UNLIMITED)
                sessions[sessionId] = channel

                call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                    try {
                        // Tell the client where to POST messages. The query string carries
                        // the session id so concurrent SSE clients don't collide.
                        write("event: endpoint\n")
                        write("data: /message?sessionId=$sessionId\n\n")
                        flush()

                        // Drain the per-session channel until the SSE client disconnects
                        // (which surfaces as an IOException on flush) or the session is closed.
                        for (payload in channel) {
                            write("event: message\n")
                            // Each line of `payload` becomes its own `data:` per the SSE
                            // line-prefix spec; encoding once avoids ambiguity if the JSON
                            // ever contains a literal newline.
                            payload.lines().forEach { line ->
                                write("data: $line\n")
                            }
                            write("\n")
                            flush()
                        }
                    } finally {
                        sessions.remove(sessionId)
                        runCatching { channel.close() }
                    }
                }
            }

            post("/message") {
                val sessionId = call.request.queryParameters["sessionId"]
                if (sessionId == null) {
                    call.respond(HttpStatusCode.BadRequest, "missing sessionId")
                    return@post
                }
                val channel = sessions[sessionId]
                if (channel == null) {
                    call.respond(HttpStatusCode.NotFound, "unknown sessionId")
                    return@post
                }
                val body = call.receiveText()

                // Accept the request synchronously (HTTP 202) and dispatch on the IO scope so
                // the POST returns immediately. The actual JSON-RPC response is pushed onto
                // the SSE channel — that's the legacy MCP transport's contract, and matches
                // what mcp-inspector and Claude Desktop expect.
                call.respond(HttpStatusCode.Accepted)
                scope.launch {
                    val response = runCatching { requestHandler(body) }.getOrNull()
                    if (response != null) {
                        runCatching { channel.send(response) }
                    }
                }
            }
        }
    }

    private companion object {
        // Bind to all loopback interfaces; `adb reverse` exposes the device-side port to the
        // host machine. 5111 is arbitrary but pinned so the Gradle task can `adb reverse`
        // deterministically without parsing the test's stdout.
        const val DEFAULT_HOST = "0.0.0.0"
        const val DEFAULT_PORT = 5111
        const val GRACE_MS = 1_000L
    }
}
