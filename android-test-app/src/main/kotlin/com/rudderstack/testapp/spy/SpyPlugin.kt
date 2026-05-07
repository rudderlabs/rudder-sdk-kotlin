package com.rudderstack.testapp.spy

import com.rudderstack.scenarioengine.domain.spy.SpyObservation
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.serialization.json.Json

/**
 * Test-only `Plugin` that observes every event flowing through the SDK at PreProcess and
 * hands a structured [SpyObservation] to its [SpySink].
 *
 * The plugin never drops or mutates events — `intercept` returns the input unchanged. v1
 * scope is *passive observation*. Filtering / mutation use cases are out of scope and would
 * need a different plugin shape (see §12.2 of the design doc).
 *
 * **Why PreProcess.** The doc's §12.2 fixes the type at PreProcess because that's the earliest
 * the event chain sees the user's data. A Terminal-level spy would observe events post-
 * enrichment, useful for a different question; the v1 step explicitly scopes to PreProcess.
 *
 * @param tag The string tag the plugin was registered under in [SpyPluginRegistry]. Surfaces
 *   on every emitted observation so the driver can route to the right test assertion.
 * @param sink Where observations go. Production: [BroadcastSpySink].
 */
internal class SpyPlugin(
    private val tag: String,
    private val sink: SpySink,
) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess
    override lateinit var analytics: Analytics

    override suspend fun intercept(event: Event): Event {
        sink.emit(buildObservation(event))
        return event
    }

    private fun buildObservation(event: Event): SpyObservation {
        val (eventType, eventName) = describe(event)
        return SpyObservation(
            tag = tag,
            kind = OBSERVATION_KIND_INTERCEPTED,
            eventType = eventType,
            eventName = eventName,
            eventJson = encodeEvent(event),
            observedAtMs = System.currentTimeMillis(),
        )
    }

    /**
     * Pull the convenience `(type, name)` pair from a concrete [Event] subclass. Sealed
     * `when` is exhaustive — adding a new event type to the SDK forces a compile error here,
     * which is the right failure mode (better than silently dropping the new type).
     *
     * Identify and Alias have no semantically meaningful name field on the event itself
     * (the user identity is on the event's userIdentityState), so [eventName] is null.
     */
    private fun describe(event: Event): Pair<String, String?> = when (event) {
        is TrackEvent -> "track" to event.event
        is ScreenEvent -> "screen" to event.screenName
        is GroupEvent -> "group" to event.groupId
        is IdentifyEvent -> "identify" to null
        is AliasEvent -> "alias" to null
    }

    /**
     * Serialize [event] to JSON using its concrete class serializer. Returns null on failure
     * rather than throwing — a malformed payload should not break the SDK's event chain.
     *
     * The sealed [Event] type uses [com.rudderstack.sdk.kotlin.core.internals.models.BaseEventSerializer]
     * for *deserialization* (a polymorphic content serializer); for serialization, dispatching
     * by concrete subclass produces a deterministic, single-shape JSON document per event kind.
     */
    private fun encodeEvent(event: Event): String? = runCatching {
        when (event) {
            is TrackEvent -> Json.encodeToString(TrackEvent.serializer(), event)
            is ScreenEvent -> Json.encodeToString(ScreenEvent.serializer(), event)
            is GroupEvent -> Json.encodeToString(GroupEvent.serializer(), event)
            is IdentifyEvent -> Json.encodeToString(IdentifyEvent.serializer(), event)
            is AliasEvent -> Json.encodeToString(AliasEvent.serializer(), event)
        }
    }.getOrNull()

    private companion object {
        const val OBSERVATION_KIND_INTERCEPTED = "intercepted"
    }
}
