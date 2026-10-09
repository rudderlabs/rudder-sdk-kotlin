package com.rudderstack.sdk.kotlin.core.server

import com.rudderstack.sdk.kotlin.core.internals.policies.CountFlushPolicy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

private const val USER_COUNT = 50
private const val OWNER = "owner"
private const val HTTP_OK = 200
private const val HTTP_SERVER_ERROR = 500
private const val HTTP_NOT_FOUND = 404

// One retry has a backoff of 3 to 6 seconds.
private const val TIMEOUT_FOR_ONE_RETRY_IN_MILLIS = 20_000L

class ServerAnalyticsEndToEndTest {

    private val receivedBatches = CopyOnWriteArrayList<ReceivedBatch>()
    private val drops = CopyOnWriteArrayList<Pair<DropReason, Int>>()

    private lateinit var dataPlane: FakeDataPlane
    private lateinit var serverAnalytics: ServerAnalytics

    @BeforeEach
    fun setUp() {
        dataPlane = FakeDataPlane(onBatch = { receivedBatches.add(it) })
        serverAnalytics = ServerAnalytics(
            ServerConfiguration(
                writeKey = "<write-key>",
                dataPlaneUrl = dataPlane.url,
                // A timer flush can upload a batch before the explicit flush, so only the count policy is active.
                flushPolicies = listOf(CountFlushPolicy()),
                dropListener = { reason, eventCount -> drops.add(reason to eventCount) },
            )
        )
    }

    @AfterEach
    fun tearDown() {
        serverAnalytics.shutdownBlocking(timeoutMillis = 0)
        dataPlane.close()
    }

    @Test
    fun `given events from many users, when the events are flushed, then each event arrives with the identity of its own call`() {
        repeat(USER_COUNT) { index ->
            val userId = "user-$index"
            val anonymousId = "anonymous-$index"
            serverAnalytics.identify(userId = userId, traits = ownedBy(userId))
            serverAnalytics.track(name = "Order Completed", userId = userId, properties = ownedBy(userId))
            serverAnalytics.screen(screenName = "Home", anonymousId = anonymousId, properties = ownedBy(anonymousId))
        }

        assertTrue(serverAnalytics.flushBlocking())

        val events = receivedBatches.flatMap { it.events }
        assertEquals(USER_COUNT * 3, events.size)
        events.forEach { event ->
            assertEquals(event.owner(), event.string("userId") ?: event.string("anonymousId"))
            assertEquals(1, listOf("userId", "anonymousId").count { it in event })
            assertEquals("server", event.string("channel"))
        }
        receivedBatches.forEach { batch ->
            assertNull(batch.headers.getFirst("AnonymousId"))
            assertEquals("gzip", batch.headers.getFirst("Content-Encoding"))
        }
        assertTrue(drops.isEmpty())
    }

    @Test
    fun `given the data plane returns 500 and then 404, when events are flushed, then the SDK retries the first batch, drops the second batch, and sends the third batch`() {
        dataPlane.respondWith(HTTP_SERVER_ERROR)
        serverAnalytics.track(name = "Retried", userId = "user-1")
        assertTrue(serverAnalytics.flushBlocking(TIMEOUT_FOR_ONE_RETRY_IN_MILLIS))

        dataPlane.respondWith(HTTP_NOT_FOUND)
        serverAnalytics.track(name = "Rejected", userId = "user-2")
        assertFalse(serverAnalytics.flushBlocking())

        serverAnalytics.track(name = "Sent After Rejection", userId = "user-3")
        assertTrue(serverAnalytics.flushBlocking())

        val expectedRequests = listOf(
            HTTP_SERVER_ERROR to "Retried",
            HTTP_OK to "Retried",
            HTTP_NOT_FOUND to "Rejected",
            HTTP_OK to "Sent After Rejection",
        )
        assertEquals(expectedRequests, receivedBatches.map { it.statusCode to it.events.single().string("event") })
        assertEquals(listOf(DropReason.REJECTED_BY_SERVER to 1), drops)
    }

    @Test
    fun `given an event is queued, when shutdown is called, then the event arrives before shutdown returns and a later event is not sent`() {
        serverAnalytics.track(name = "Before Shutdown", userId = "user-1")

        assertTrue(serverAnalytics.shutdownBlocking())
        serverAnalytics.track(name = "After Shutdown", userId = "user-1")

        assertFalse(serverAnalytics.flushBlocking())
        assertEquals(listOf("Before Shutdown"), receivedBatches.flatMap { it.events }.map { it.string("event") })
    }

    private fun ownedBy(id: String) = buildJsonObject { put(OWNER, id) }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.content

    private fun JsonObject.owner(): String? {
        val traits = this["context"]?.jsonObject?.get("traits")?.jsonObject
        return (traits ?: this["properties"]?.jsonObject)?.string(OWNER)
    }
}
