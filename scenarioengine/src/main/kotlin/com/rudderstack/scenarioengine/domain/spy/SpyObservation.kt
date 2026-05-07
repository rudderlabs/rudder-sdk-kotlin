package com.rudderstack.scenarioengine.domain.spy

import kotlinx.serialization.Serializable

/**
 * One observation emitted by a SUT-side `SpyPlugin` and surfaced to the driver via the
 * [com.rudderstack.scenarioengine.ipc.Commands.EVENT_TYPE_SDK_EVENT] broadcast channel.
 *
 * Carries enough for the common assertion shape — *"the spy with this tag saw a track named
 * Tap"* — without forcing every test to walk the full SDK event JSON. [eventJson] is populated
 * on a best-effort basis for tests that need to assert on properties / context fields the
 * convenience fields don't expose.
 *
 * Sealed by structure rather than by class hierarchy: more observation kinds (state mutations,
 * session boundaries) will land here as additional `kind` values once the SDK exposes the
 * corresponding seams (§17). For now the only kind is `"intercepted"`.
 *
 * @param tag The string tag the spy was registered under via [com.rudderstack.scenarioengine.domain.step.Step.AddSpyPlugin].
 * @param kind The observation category — `"intercepted"` for an event seen at PreProcess. Reserved for future kinds.
 * @param eventType One of `track`, `screen`, `identify`, `group`, `alias` — null if not derivable.
 * @param eventName The event's primary name (track event name, screen name, group id) — null when not applicable.
 * @param eventJson The full event serialized to JSON when serialization succeeds, otherwise null.
 * @param observedAtMs Wall-clock time in milliseconds when the SpyPlugin saw the event.
 */
@Serializable
data class SpyObservation(
    val tag: String,
    val kind: String,
    val eventType: String? = null,
    val eventName: String? = null,
    val eventJson: String? = null,
    val observedAtMs: Long,
)
