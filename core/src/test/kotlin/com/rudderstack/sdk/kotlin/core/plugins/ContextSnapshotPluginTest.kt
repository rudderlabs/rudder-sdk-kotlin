package com.rudderstack.sdk.kotlin.core.plugins

import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val EVENT_NAME = "Sample Event"

class ContextSnapshotPluginTest {

    private lateinit var plugin: ContextSnapshotPlugin

    @BeforeEach
    fun setup() {
        plugin = ContextSnapshotPlugin()
    }

    @Test
    fun `given a context with base and custom keys, when intercepted, then only present base keys are recorded`() =
        runTest {
            val event = provideEvent().also {
                it.context = buildJsonObject {
                    put("library", buildJsonObject { put("name", "sample") })
                    put("timezone", "Asia/Kolkata")
                    put("campaign", "not-a-base-key")
                }
            }

            plugin.intercept(event)

            val snapshot = plugin.consumeSnapshot(event.messageId)
            assertEquals(setOf("library", "timezone"), snapshot?.keys)
        }

    @Test
    fun `given a recorded snapshot, when consumed twice, then the second consume returns null`() = runTest {
        val event = provideEvent().also { it.context = buildJsonObject { put("timezone", "Asia/Kolkata") } }
        plugin.intercept(event)

        plugin.consumeSnapshot(event.messageId)

        assertNull(plugin.consumeSnapshot(event.messageId))
    }

    @Test
    fun `given a recorded snapshot, when consumed with another messageId, then it returns null and clears the slot`() =
        runTest {
            val event = provideEvent().also { it.context = buildJsonObject { put("timezone", "Asia/Kolkata") } }
            plugin.intercept(event)

            assertNull(plugin.consumeSnapshot("another-message-id"))
            assertNull(plugin.consumeSnapshot(event.messageId))
        }
}

private fun provideEvent(): Event = TrackEvent(
    event = EVENT_NAME,
    properties = emptyJsonObject,
)
