package com.rudderstack.sdk.kotlin.android.e2e.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.util.concurrent.TimeUnit

/**
 * Parses the request body as a JSON object.
 */
fun RecordedRequest.parseBatchPayload(): JsonObject {
    val bodyString = body.readUtf8()
    return Json.parseToJsonElement(bodyString).jsonObject
}

/**
 * Extracts the "batch" array from the top-level batch payload.
 */
fun JsonObject.getBatchArray(): JsonArray {
    return this["batch"]?.jsonArray
        ?: throw AssertionError("Expected 'batch' array in payload, got keys: ${keys}")
}

/**
 * Waits for the first POST request to /v1/batch, skipping any other requests
 * (e.g., GET /sourceConfig).
 *
 * @param server The MockWebServer to take requests from.
 * @param timeoutSeconds Maximum time to wait for the batch request.
 * @return The recorded batch request.
 * @throws AssertionError if no batch request is received within the timeout.
 */
fun waitForBatchRequest(server: MockWebServer, timeoutSeconds: Long = 10): RecordedRequest {
    val deadline = System.currentTimeMillis() + timeoutSeconds * 1000
    while (System.currentTimeMillis() < deadline) {
        val remainingMs = deadline - System.currentTimeMillis()
        if (remainingMs <= 0) break

        val request = server.takeRequest(remainingMs, TimeUnit.MILLISECONDS) ?: break

        if (request.method == "POST" && request.path?.contains("/v1/batch") == true) {
            return request
        }
    }
    throw AssertionError("Did not receive a POST /v1/batch request within ${timeoutSeconds}s")
}
