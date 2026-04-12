package com.rudderstack.sdk.kotlin.android.e2e

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.e2e.utils.MockServerDispatcher
import com.rudderstack.sdk.kotlin.android.e2e.utils.TestAnalyticsFactory
import com.rudderstack.sdk.kotlin.android.e2e.utils.getBatchArray
import com.rudderstack.sdk.kotlin.android.e2e.utils.parseBatchPayload
import com.rudderstack.sdk.kotlin.android.e2e.utils.waitForBatchRequest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackEventE2ETest {

    private lateinit var server: MockWebServer
    private lateinit var analytics: Analytics

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = MockServerDispatcher()
        server.start()
    }

    @After
    fun tearDown() {
        if (::analytics.isInitialized) {
            analytics.shutdown()
        }
        server.shutdown()
    }

    @Test
    fun trackEventDeliversCorrectPayloadToDataPlane() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val baseUrl = server.url("/").toString()

        analytics = TestAnalyticsFactory.create(
            application = app,
            mockServerUrl = baseUrl,
        )

        analytics.track(
            name = "Purchase",
            properties = buildJsonObject {
                put("amount", 99)
                put("currency", "USD")
            }
        )

        val batchRequest = waitForBatchRequest(server, timeoutSeconds = 10)

        // Assert request method and path
        assertEquals("POST", batchRequest.method)
        assertTrue(
            "Expected path to contain /v1/batch but was: ${batchRequest.path}",
            batchRequest.path!!.contains("/v1/batch")
        )

        // Assert Authorization header is present
        val authHeader = batchRequest.getHeader("Authorization")
        assertNotNull("Expected Authorization header", authHeader)
        assertTrue(
            "Expected Authorization to start with 'Basic ' but was: $authHeader",
            authHeader!!.startsWith("Basic ")
        )

        // Parse batch payload
        val payload = batchRequest.parseBatchPayload()
        val batch = payload.getBatchArray()
        assertTrue("Expected at least 1 event in batch, got ${batch.size}", batch.isNotEmpty())

        // Find the track event (skip any system events if present)
        val trackEvent = batch.firstOrNull {
            it.jsonObject["type"]?.jsonPrimitive?.content == "track" &&
                it.jsonObject["event"]?.jsonPrimitive?.content == "Purchase"
        }?.jsonObject
            ?: throw AssertionError(
                "Expected a track event with name 'Purchase' in batch. " +
                    "Events: ${batch.map { it.jsonObject["type"]?.jsonPrimitive?.content }}"
            )

        // Assert event type and name
        assertEquals("track", trackEvent["type"]?.jsonPrimitive?.content)
        assertEquals("Purchase", trackEvent["event"]?.jsonPrimitive?.content)

        // Assert properties
        val properties = trackEvent["properties"]?.jsonObject
        assertNotNull("Expected properties in track event", properties)
        assertEquals(99, properties!!["amount"]?.jsonPrimitive?.int)
        assertEquals("USD", properties["currency"]?.jsonPrimitive?.content)

        // Assert required fields
        assertNotNull("Expected messageId", trackEvent["messageId"])
        assertNotNull("Expected anonymousId", trackEvent["anonymousId"])
        assertNotNull("Expected originalTimestamp", trackEvent["originalTimestamp"])
        assertNotNull("Expected context", trackEvent["context"])

        // Assert sentAt at batch level
        assertNotNull("Expected sentAt in batch payload", payload["sentAt"])
    }
}
