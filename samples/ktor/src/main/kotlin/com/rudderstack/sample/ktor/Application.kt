package com.rudderstack.sample.ktor

import com.rudderstack.sdk.kotlin.core.server.ServerAnalytics
import com.rudderstack.sdk.kotlin.core.server.ServerConfiguration
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val DEFAULT_PORT = 8080

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: DEFAULT_PORT
    embeddedServer(Netty, port = port, module = Application::module).start(wait = true)
}

fun Application.module() {
    // One instance for the life of the process. Each request thread uses this instance.
    val analytics = ServerAnalytics(
        ServerConfiguration(
            writeKey = requiredEnvironmentVariable("RUDDERSTACK_WRITE_KEY"),
            dataPlaneUrl = requiredEnvironmentVariable("RUDDERSTACK_DATA_PLANE_URL"),
            dropListener = { reason, eventCount -> log.warn("RudderStack dropped {} event(s): {}", eventCount, reason) },
        )
    )

    // Ktor raises this event after the server stops. The call sends each queued event before the process exits.
    monitor.subscribe(ApplicationStopped) {
        val isEachEventSent = analytics.shutdownBlocking()
        log.info("RudderStack shutdown complete. Each event sent: {}", isEachEventSent)
    }

    routing {
        post("/orders") {
            val userId = call.request.queryParameters["userId"]
            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "The userId query parameter is missing")
                return@post
            }

            // The user of the request goes on the event call. The SDK stores no user.
            analytics.track(
                name = "Order Completed",
                userId = userId,
                properties = buildJsonObject { put("revenue", 30) },
            )
            call.respond(HttpStatusCode.Accepted)
        }
    }
}

private fun requiredEnvironmentVariable(name: String): String =
    checkNotNull(System.getenv(name)) { "Set the $name environment variable" }
