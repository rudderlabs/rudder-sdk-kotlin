package com.rudderstack.sdk.kotlin.core.internals.models

import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

private const val RESERVED_KEY = "consentManagement"
private const val CAPTURED_MARKER = "captured-at-creation"
private val CAPTURED = mapOf(RESERVED_KEY to JsonPrimitive(CAPTURED_MARKER))

/**
 * Two invariants on [Event] that fail silently if broken.
 *
 * [Event.copy] rebuilds an event field by field, and `PluginInteractor` copies the event before
 * every plugin in the chain - not only at the device-mode handoff. A field missed in that rebuild
 * is therefore lost at the very first plugin, on every delivery path, with no compile error and no
 * crash. The stamper, the terminal guard, the consent gate and the handoff would each find nothing
 * and fall back to live state, so an event would be recorded and delivered against whatever consent
 * happened to be in force at the time rather than the consent it was created under.
 *
 * The same field must never be serialized. It is internal bookkeeping, not payload, and a missing
 * `@Transient` would leak it to the data plane unnoticed.
 */
class EventCopyTest {

    @Test
    fun `given every event type carrying captured reserved context, when copied, then the copy retains it`() {
        allEventTypes().forEach { event ->
            event.capturedReservedContext = CAPTURED

            val copy = event.copy<Event>()

            assertEquals(
                CAPTURED,
                copy.capturedReservedContext,
                "${event::class.simpleName} lost its captured reserved context when copied",
            )
        }
    }

    @Test
    fun `given an event carrying captured reserved context, when serialized, then it never reaches the payload`() {
        val event = TrackEvent(event = "payload-check", properties = emptyJsonObject)
            .withBaseData()
            .also { it.capturedReservedContext = CAPTURED }

        val payload = event.encodeToString()

        assertFalse(payload.contains("capturedReservedContext"), "internal field reached the payload: $payload")
        assertFalse(payload.contains(CAPTURED_MARKER), "captured value reached the payload: $payload")
    }

    private fun allEventTypes(): List<Event> = listOf(
        TrackEvent(event = "track", properties = emptyJsonObject),
        ScreenEvent(screenName = "screen", properties = emptyJsonObject),
        GroupEvent(groupId = "group", traits = emptyJsonObject),
        IdentifyEvent(),
        AliasEvent(previousId = "previous"),
    ).map { it.withBaseData() }
}

/** Populates the lateinit fields [Event.copy] reads, so a bare event can be copied at all. */
private fun <T : Event> T.withBaseData(): T = apply {
    integrations = emptyJsonObject
    anonymousId = "anonymous-id"
    channel = PlatformType.Mobile
}
