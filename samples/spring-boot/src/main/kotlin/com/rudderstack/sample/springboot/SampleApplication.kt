package com.rudderstack.sample.springboot

import com.rudderstack.sdk.kotlin.core.server.ServerAnalytics
import com.rudderstack.sdk.kotlin.core.server.ServerConfiguration
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class SampleApplication {

    private val logger = LoggerFactory.getLogger(SampleApplication::class.java)

    // A bean is a singleton. Spring calls `shutdownBlocking` when the context closes.
    // The call sends each queued event before the process exits.
    @Bean(destroyMethod = "shutdownBlocking")
    fun serverAnalytics(
        @Value("\${rudderstack.write-key}") writeKey: String,
        @Value("\${rudderstack.data-plane-url}") dataPlaneUrl: String,
    ) = ServerAnalytics(
        ServerConfiguration(
            writeKey = writeKey,
            dataPlaneUrl = dataPlaneUrl,
            dropListener = { reason, eventCount -> logger.warn("RudderStack dropped {} event(s): {}", eventCount, reason) },
        )
    )
}

@RestController
class OrderController(private val analytics: ServerAnalytics) {

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun createOrder(@RequestParam userId: String) {
        // The user of the request goes on the event call. The SDK stores no user.
        analytics.track(
            name = "Order Completed",
            userId = userId,
            properties = buildJsonObject { put("revenue", 30) },
        )
    }
}

fun main(args: Array<String>) {
    runApplication<SampleApplication>(*args)
}
