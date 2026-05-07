package com.rudderstack.scenarioengine.domain.step

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * The kind of analytics event a [Step.WaitForEvent] / [Step.AssertNoEvent] is matching against.
 *
 * Renamed from `EventType` in the design doc to avoid a name collision with the SDK's own
 * `com.rudderstack.sdk.kotlin.core.internals.models.EventType`. The values mirror the SDK's
 * five event kinds 1:1.
 */
enum class StepEventType { TRACK, SCREEN, IDENTIFY, GROUP, ALIAS }

/**
 * The piece of in-SDK identity state a [Step.AssertState] is asserting on.
 * Resolved at runtime by [com.rudderstack.scenarioengine.domain.helper.StateProbe].
 */
enum class StateField { ANONYMOUS_ID, USER_ID, SESSION_ID }

/**
 * One typed, observable action in a scenario.
 *
 * `Step` is a value, not a behavior — it describes *what* should happen, never *how*. The
 * Interpreter is the single place that translates a Step into helper calls. Adding a new
 * Step subtype is the open/closed extension point for the engine: the sealed hierarchy makes
 * the compiler force every dispatch site to handle the new case.
 *
 * Steps fall into a handful of categories:
 *  - **Init / teardown:** [Init], [Reset], [Shutdown]
 *  - **Events:** [Track], [Screen], [Identify], [Group], [Alias], [Flush]
 *  - **Session:** [StartSession], [EndSession]
 *  - **App lifecycle:** [Background], [Foreground], [Kill], [ForceStop], [ColdStart], [ClearAppData]
 *  - **System state:** [DeepLink], [NetworkOffline], [NetworkOnline], [DozeOn], [DozeOff], [LocaleChange]
 *  - **Faults:** [Crash], [ThrowOnMainThread], [NativeCrash]
 *  - **Assertions:** [WaitForBatch], [WaitForEvent], [AssertField], [AssertNoEvent], [AssertState]
 *  - **State export/import:** [SnapshotState], [ImportState]
 *  - **Test-only plugins:** [AddSpyPlugin], [RemoveSpyPlugin]
 */
sealed class Step {

    /**
     * Initialize the SDK with the given configuration. Carries every knob the engine sets on
     * the SDK for testability (lifecycle tracking flags, session config, mock server URL, etc.).
     *
     * @param mockServerUrl Both `dataPlaneUrl` and `controlPlaneUrl` on the SDK's Configuration are pointed at this URL.
     * @param flushAt Overrides `flushPolicies` to a single `CountFlushPolicy(flushAt = N)` so assertions can be tight.
     */
    data class Init(
        val writeKey: String,
        val mockServerUrl: String,
        val trackApplicationLifecycleEvents: Boolean = false,
        val trackDeepLinks: Boolean = false,
        val trackActivities: Boolean = false,
        val automaticSessionTracking: Boolean = false,
        val sessionTimeoutMs: Long? = null,
        val flushAt: Int = 1,
    ) : Step()

    /**
     * Reset some or all of the SDK's identity state.
     *
     * Mirrors the four flags on the Android SDK's `ResetEntries` 1:1 so the engine exposes the
     * full reset contract without inventing its own subset. All default to `true` — that's the
     * SDK's "full reset" semantics, and the canonical use of [Reset] in scenarios is to bring
     * the SDK back to a known-fresh state.
     *
     * **Doc deviation.** §4.1 shows `Reset(resetSession: Boolean)`. The implementation expanded
     * to four flags after reading `com.rudderstack.sdk.kotlin.android.models.reset.ResetEntries`,
     * which exposes all four on the public API.
     *
     * @param anonymousId If true, generate a new anonymous ID.
     * @param userId If true, clear the user ID.
     * @param traits If true, clear user traits.
     * @param session If true, refresh the active session (Android-specific).
     */
    data class Reset(
        val anonymousId: Boolean = true,
        val userId: Boolean = true,
        val traits: Boolean = true,
        val session: Boolean = true,
    ) : Step()

    /** Tear down the SDK instance. After this, further event Steps are no-ops until the next [Init]. */
    data object Shutdown : Step()

    /** Send a Track event with the given name and properties. */
    data class Track(
        val name: String,
        val properties: JsonObject = JsonObject(emptyMap()),
    ) : Step()

    /** Send a Screen event. */
    data class Screen(
        val name: String,
        val category: String? = null,
        val properties: JsonObject = JsonObject(emptyMap()),
    ) : Step()

    /** Send an Identify event with the given userId and traits. */
    data class Identify(
        val userId: String,
        val traits: JsonObject = JsonObject(emptyMap()),
    ) : Step()

    /** Associate the current user with a group. */
    data class Group(
        val groupId: String,
        val traits: JsonObject = JsonObject(emptyMap()),
    ) : Step()

    /**
     * Merge two identities under a single user.
     *
     * @param previousId If null, the SDK resolves it from current user state.
     */
    data class Alias(
        val newId: String,
        val previousId: String? = null,
    ) : Step()

    /** Force the SDK to drain its queue to the data plane immediately. */
    data object Flush : Step()

    /**
     * Start a session.
     *
     * @param sessionId If non-null, the session runs in manual-mode with this id; otherwise the SDK auto-assigns.
     */
    data class StartSession(val sessionId: Long? = null) : Step()

    /** End the current session. */
    data object EndSession : Step()

    /** Move the SUT to background (HOME key). */
    data object Background : Step()

    /** Foreground the SUT (start its main activity). */
    data object Foreground : Step()

    /** Kill the SUT process via `am kill`. */
    data object Kill : Step()

    /** Force-stop the SUT process via `am force-stop`. */
    data object ForceStop : Step()

    /** Force-stop then re-launch the SUT (full cold start). */
    data object ColdStart : Step()

    /** Wipe the SUT's app data via `pm clear`. */
    data object ClearAppData : Step()

    /** Deliver a deep link to the SUT (`am start … VIEW`). */
    data class DeepLink(val uri: String, val referrer: String? = null) : Step()

    /** Disable the device's network. */
    data object NetworkOffline : Step()

    /** Re-enable the device's network. */
    data object NetworkOnline : Step()

    /** Force the device into Doze (`dumpsys deviceidle force-idle`). */
    data object DozeOn : Step()

    /** Take the device out of Doze. */
    data object DozeOff : Step()

    /** Change the device locale (e.g. "fr-FR"). */
    data class LocaleChange(val tag: String) : Step()

    /** Inject a JVM-level crash into the SUT process. */
    data object Crash : Step()

    /** Throw on the SUT's main thread (different code path from a generic [Crash]). */
    data object ThrowOnMainThread : Step()

    /** Inject a native (JNI/segfault) crash into the SUT process. */
    data object NativeCrash : Step()

    /**
     * Block until the next batch arrives at the mock plane, or fail after the timeout.
     *
     * @param timeoutMs Wall-clock cap in milliseconds.
     */
    data class WaitForBatch(val timeoutMs: Long = 10_000) : Step()

    /**
     * Block until an event matching the given criteria appears on the mock plane.
     *
     * @param match Implicit-AND list of [FieldMatch] predicates over the event's JSON.
     *              Empty list ⇒ match the first event of the right type/name.
     */
    data class WaitForEvent(
        val type: StepEventType,
        val name: String? = null,
        val timeoutMs: Long = 10_000,
        val match: List<FieldMatch> = emptyList(),
    ) : Step()

    /**
     * Assert a single dotted-path field equals the expected JSON value, against the most-recent
     * event captured by the mock plane. Intended for the simple equality case; for richer
     * assertions, use [WaitForEvent] with a [FieldMatch] list.
     */
    data class AssertField(val path: String, val expected: JsonElement) : Step()

    /**
     * Assert that no event of the given type/name arrives within the given window.
     *
     * @param windowMs How long to watch before declaring success.
     */
    data class AssertNoEvent(
        val type: StepEventType,
        val name: String? = null,
        val windowMs: Long = 2_000,
    ) : Step()

    /** Assert a piece of in-SDK identity state matches an expected value (or null). */
    data class AssertState(val field: StateField, val expected: String?) : Step()

    /**
     * Capture a versioned binary blob of the SDK's current state (anonymousId, userId,
     * traits, session, queued events). Used by time-travel and seeding scenarios.
     */
    data object SnapshotState : Step()

    /**
     * Restore SDK state from a blob previously produced by [SnapshotState]. The blob format
     * is internal; mismatched versions fail loudly rather than silently coercing.
     */
    data class ImportState(val blob: ByteArray) : Step() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ImportState) return false
            return blob.contentEquals(other.blob)
        }

        override fun hashCode(): Int = blob.contentHashCode()
    }

    /**
     * Register a test-only `SpyPlugin` in the SUT under a string tag. Spies observe internal
     * SDK state (intercepts, identity transitions) that the wire-side mock plane cannot see.
     */
    data class AddSpyPlugin(val tag: String) : Step()

    /** Remove a previously-added SpyPlugin. */
    data class RemoveSpyPlugin(val tag: String) : Step()
}
