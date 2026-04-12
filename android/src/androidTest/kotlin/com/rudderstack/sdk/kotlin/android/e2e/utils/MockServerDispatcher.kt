package com.rudderstack.sdk.kotlin.android.e2e.utils

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

/**
 * Routes MockWebServer requests based on path:
 * - GET /sourceConfig → valid source config JSON (SDK fetches this on init)
 * - POST /v1/batch → 200 OK (event upload endpoint)
 * - Everything else → 404
 */
class MockServerDispatcher : Dispatcher() {

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = request.path ?: return MockResponse().setResponseCode(404)

        return when {
            path.contains("/sourceConfig") -> MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(SOURCE_CONFIG_RESPONSE)

            path.contains("/v1/batch") -> MockResponse()
                .setResponseCode(200)
                .setBody("OK")

            else -> MockResponse().setResponseCode(404)
        }
    }

    companion object {

        private val SOURCE_CONFIG_RESPONSE = """
            {
              "source": {
                "id": "test-source-id",
                "name": "Test Source",
                "writeKey": "test-write-key",
                "enabled": true,
                "workspaceId": "test-workspace-id",
                "updatedAt": "2024-01-01T00:00:00.000Z",
                "config": {
                  "statsCollection": {
                    "errors": { "enabled": false },
                    "metrics": { "enabled": false }
                  }
                },
                "destinations": []
              }
            }
        """.trimIndent()
    }
}
