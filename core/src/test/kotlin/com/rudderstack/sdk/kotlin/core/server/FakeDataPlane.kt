package com.rudderstack.sdk.kotlin.core.server

import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.zip.GZIPInputStream

private const val LOOPBACK_ADDRESS = "127.0.0.1"
private const val BATCH_PATH = "/v1/batch"
private const val CONTENT_ENCODING = "Content-Encoding"
private const val GZIP = "gzip"

/**
 * A local data plane for tests. It reports each batch request and answers with the next planned status code.
 */
internal class FakeDataPlane(private val onBatch: (ReceivedBatch) -> Unit) : AutoCloseable {

    private val plannedStatusCodes = ConcurrentLinkedQueue<Int>()
    private val server = HttpServer.create(InetSocketAddress(LOOPBACK_ADDRESS, 0), 0).apply {
        createContext(BATCH_PATH, ::handle)
        start()
    }

    val url: String = "http://$LOOPBACK_ADDRESS:${server.address.port}"

    /**
     * Plans the status codes of the next requests. A request with no planned status code gets 200.
     */
    fun respondWith(vararg statusCodes: Int) {
        plannedStatusCodes.addAll(statusCodes.toList())
    }

    override fun close() {
        server.stop(0)
    }

    private fun handle(exchange: HttpExchange) {
        val statusCode = plannedStatusCodes.poll() ?: HttpURLConnection.HTTP_OK
        try {
            onBatch(ReceivedBatch(statusCode, exchange.requestHeaders, exchange.readBody()))
            exchange.sendResponseHeaders(statusCode, -1)
        } finally {
            exchange.close()
        }
    }

    private fun HttpExchange.readBody(): String {
        val stream = if (requestHeaders.getFirst(CONTENT_ENCODING) == GZIP) GZIPInputStream(requestBody) else requestBody
        return stream.bufferedReader().use { it.readText() }
    }
}

internal class ReceivedBatch(val statusCode: Int, val headers: Headers, val body: String) {

    val events: List<JsonObject> by lazy {
        LenientJson.parseToJsonElement(body).jsonObject.getValue("batch").jsonArray.map { it.jsonObject }
    }
}
