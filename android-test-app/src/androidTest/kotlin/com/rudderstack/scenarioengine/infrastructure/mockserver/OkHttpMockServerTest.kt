package com.rudderstack.scenarioengine.infrastructure.mockserver

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rudderstack.scenarioengine.domain.transport.MockResponseSpec
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validates [OkHttpMockServer]'s default route table (Step 4 exit criterion) and the
 * [installRoute] override path.
 *
 * Doesn't go through the SUT — this is a pure-driver-side adapter test. Uses a real
 * [OkHttpClient] to drive HTTP traffic against the loopback port the mock binds.
 */
@RunWith(AndroidJUnit4::class)
class OkHttpMockServerTest {

    private val server = OkHttpMockServer()
    private val client = OkHttpClient()

    @Before fun setUp() = runBlocking { server.start() }

    @After fun tearDown() = runBlocking { server.shutdown() }

    @Test
    fun default_routes_serve_sourceConfig_batch_and_404() {
        val sourceConfig = client.newCall(
            Request.Builder().url("${server.baseUrl}/sourceConfig").build(),
        ).execute()
        assertEquals(200, sourceConfig.code)
        val body = sourceConfig.body?.string().orEmpty()
        assertTrue(
            "default /sourceConfig body should declare a source: $body",
            body.contains("\"source\""),
        )

        val batch = client.newCall(
            Request.Builder()
                .url("${server.baseUrl}/v1/batch")
                .post("[]".toRequestBody("application/json".toMediaType()))
                .build(),
        ).execute()
        assertEquals(200, batch.code)
        assertEquals("OK", batch.body?.string())

        val unknown = client.newCall(
            Request.Builder().url("${server.baseUrl}/something/random").build(),
        ).execute()
        assertEquals(404, unknown.code)
    }

    @Test
    fun installRoute_overrides_default_response_for_path() {
        server.installRoute("/sourceConfig", MockResponseSpec.Static(status = 503, body = "down"))
        val response = client.newCall(
            Request.Builder().url("${server.baseUrl}/sourceConfig").build(),
        ).execute()
        assertEquals(503, response.code)
        assertEquals("down", response.body?.string())
    }
}
