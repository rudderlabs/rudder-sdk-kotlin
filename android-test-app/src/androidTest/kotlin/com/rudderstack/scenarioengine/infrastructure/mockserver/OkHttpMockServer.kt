package com.rudderstack.scenarioengine.infrastructure.mockserver

import com.rudderstack.scenarioengine.domain.transport.MockResponseSpec
import com.rudderstack.scenarioengine.domain.transport.MockServer
import com.rudderstack.scenarioengine.domain.transport.RecordedRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.mockwebserver.RecordedRequest as MockWebServerRecordedRequest

/**
 * Android implementation of [MockServer] backed by [MockWebServer].
 *
 * Owns a `ConcurrentHashMap`-based route table that defaults to the §9.2 minimum:
 * `GET /sourceConfig` returns a single-source-no-destinations config; `POST /v1/batch`
 * returns `200 "OK"`; everything else falls through to `404`. Scenarios override per path
 * via [installRoute] — last write wins.
 *
 * Every dispatched request is mirrored into a non-replaying [MutableSharedFlow] so multiple
 * subscribers (e.g. `MockPlane.waitForBatch`, a request-count probe) can observe the same
 * stream. Subscribers must be attached **before** the request is made — there is no replay.
 *
 * `MockResponseSpec.Sequence` advances a per-path cursor on each dispatch. Once exhausted
 * the path returns 404, which matches the design intent: the test should explicitly install
 * a tail response if it wants traffic past the sequence.
 *
 * `start()` is idempotent. The server is bound to a random loopback port; [baseUrl] is only
 * meaningful after [start].
 */
class OkHttpMockServer : MockServer {

    private val server = MockWebServer()
    private val routes = ConcurrentHashMap<String, MockResponseSpec>()
    private val sequenceCursors = ConcurrentHashMap<String, AtomicInteger>()
    private val requests = MutableSharedFlow<RecordedRequest>(replay = 0, extraBufferCapacity = REQUEST_BUFFER)
    private val started = AtomicBoolean(false)

    override val baseUrl: String
        get() = server.url("/").toString().trimEnd('/')

    override suspend fun start() {
        if (!started.compareAndSet(false, true)) return
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: MockWebServerRecordedRequest): MockResponse {
                val recorded = request.toDomain()
                requests.tryEmit(recorded)
                return resolve(recorded.path)
            }
        }
        installDefaultRoutes()
        withContext(Dispatchers.IO) { server.start() }
    }

    override suspend fun shutdown() {
        if (!started.compareAndSet(true, false)) return
        withContext(Dispatchers.IO) { server.shutdown() }
    }

    override fun observeRequests(): Flow<RecordedRequest> = requests.asSharedFlow()

    override fun installRoute(path: String, response: MockResponseSpec) {
        routes[path] = response
        sequenceCursors.remove(path) // reset progression when the spec is replaced
    }

    private fun installDefaultRoutes() {
        installRoute("/sourceConfig", MockResponseSpec.Static(status = 200, body = DEFAULT_SOURCE_CONFIG))
        installRoute("/v1/batch", MockResponseSpec.Static(status = 200, body = "OK"))
    }

    private fun resolve(path: String): MockResponse {
        val spec = routes[path] ?: return MockResponse().setResponseCode(404)
        return when (spec) {
            is MockResponseSpec.Static -> spec.toMockResponse()
            is MockResponseSpec.Sequence -> {
                val cursor = sequenceCursors.getOrPut(path) { AtomicInteger(0) }
                val idx = cursor.getAndIncrement()
                if (idx < spec.responses.size) spec.responses[idx].toMockResponse()
                else MockResponse().setResponseCode(404)
            }
            is MockResponseSpec.Delayed -> spec.response.toMockResponse()
                .setBodyDelay(spec.delayMs, TimeUnit.MILLISECONDS)
        }
    }

    private fun MockResponseSpec.Static.toMockResponse(): MockResponse =
        MockResponse().setResponseCode(status).setBody(body)

    private fun MockWebServerRecordedRequest.toDomain(): RecordedRequest {
        val headerMap = headers.names().associateWith { name -> headers.get(name).orEmpty() }
        return RecordedRequest(
            method = method.orEmpty(),
            path = path.orEmpty(),
            headers = headerMap,
            body = body.readUtf8(),
        )
    }

    private companion object {
        const val REQUEST_BUFFER = 256
        const val DEFAULT_SOURCE_CONFIG =
            """{"source":{"id":"test-source","enabled":true,"config":{},"destinations":[]}}"""
    }
}
