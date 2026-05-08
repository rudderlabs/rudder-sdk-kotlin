package com.rudderstack.scenarioengine.scenarios.lifecycle

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rudderstack.scenarioengine.application.dsl.rudderScenario
import com.rudderstack.scenarioengine.domain.spy.SpyObservation
import com.rudderstack.scenarioengine.domain.step.Step
import com.rudderstack.scenarioengine.domain.step.StepEventType
import com.rudderstack.scenarioengine.scenarios.ScenarioRunnerTest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Non-destructive lifecycle Step types: [Step.Background] and [Step.Foreground].
 *
 * Both are wired through [com.rudderstack.scenarioengine.domain.helper.LifecycleControl] (the
 * adapter — `AdbLifecycleControl` — has been live since Step 4, but no scenario invoked it
 * via the interpreter until now).
 *
 * Destructive lifecycle ops ([Step.Kill], [Step.ForceStop], [Step.ColdStart],
 * [Step.ClearAppData]) ride the same adapter but are gated on Step 6b's manifest/AGP fix —
 * see `src/androidTest/AndroidManifest.xml`.
 *
 * **Why lifecycle-event tracking.** The interesting thing to assert about Background /
 * Foreground isn't "the method got called" — that's already tested by AdbLifecycleControlTest.
 * The real question is "does the SDK observe the transition?" Enabling
 * `trackApplicationLifecycleEvents` makes the SDK emit `"Application Backgrounded"` /
 * `"Application Opened"` Track events; those events on the wire are the proof.
 */
@RunWith(AndroidJUnit4::class)
class LifecycleNonDestructiveScenariosTest : ScenarioRunnerTest() {

    @Test
    fun background_emits_application_backgrounded_event() {
        runScenario(rudderScenario(name = "lifecycle.background") {
            initBlock = {
                Step.Init(
                    writeKey = "test-write-key",
                    mockServerUrl = "",
                    trackApplicationLifecycleEvents = true,
                )
            }
            background()
            waitForEvent(type = StepEventType.TRACK, name = APPLICATION_BACKGROUNDED)
        })
    }

    @Test
    fun foreground_emits_application_opened_event() {
        runScenario(rudderScenario(name = "lifecycle.foreground") {
            initBlock = {
                Step.Init(
                    writeKey = "test-write-key",
                    mockServerUrl = "",
                    trackApplicationLifecycleEvents = true,
                )
            }
            // The launcher already foregrounded the app in the runner's `@Before`; background
            // first so the next foreground() is observable as a transition.
            background()
            waitForEvent(type = StepEventType.TRACK, name = APPLICATION_BACKGROUNDED)
            foreground()
            waitForEvent(type = StepEventType.TRACK, name = APPLICATION_OPENED)
        })
    }

    /**
     * **§15.2 doc-named L2 scenario** ("lifecycle.cold_start_to_background_to_foreground").
     *
     * Walks the full first-launch → background → foreground arc and asserts the
     * `properties.from_background` flag flips correctly:
     *
     *  - Cold-start init fires the *first* `Application Opened` with `from_background = false`
     *    (`AndroidLifecyclePlugin` flips its `firstLaunch` AtomicBoolean once on the first
     *    `onStart`; see `AndroidLifecyclePlugin.kt:68`).
     *  - Background fires `Application Backgrounded`.
     *  - Foreground fires the *second* `Application Opened` — now with `from_background = true`.
     *
     * Distinct from [foreground_emits_application_opened_event]: that scenario only checks the
     * event names fire; this one is the load-bearing assertion that the flag itself is correct,
     * which is what consumers of these events actually rely on.
     */
    @Test
    fun cold_start_then_background_then_foreground_flips_from_background_flag() {
        runScenario(rudderScenario(name = "lifecycle.cold_start_to_background_to_foreground") {
            initBlock = {
                Step.Init(
                    writeKey = "test-write-key",
                    mockServerUrl = "",
                    trackApplicationLifecycleEvents = true,
                )
            }
            // First Application Opened comes from `AndroidLifecyclePlugin.onStart` firing right
            // after Init registers the process-lifecycle observer (the SUT process is already
            // started by the runner's @Before coldStart). firstLaunch is true → from_background = false.
            waitForEvent(type = StepEventType.TRACK, name = APPLICATION_OPENED)
            assertField(path = "properties.from_background", expected = JsonPrimitive(false))

            background()
            waitForEvent(type = StepEventType.TRACK, name = APPLICATION_BACKGROUNDED)

            foreground()
            // Second Application Opened — firstLaunch is now false → from_background = true.
            waitForEvent(type = StepEventType.TRACK, name = APPLICATION_OPENED)
            assertField(path = "properties.from_background", expected = JsonPrimitive(true))
        })
    }

    /**
     * Long backgrounding (> [SHORT_SESSION_TIMEOUT_MS]) crosses the auto-session timeout boundary;
     * the foreground transition rotates the session. Asserted by capturing each track event's
     * `context.sessionId` via a SpyPlugin and comparing pre / post.
     *
     * **§18 doc-named L2 scenario** ("background_long_then_foreground_rotates_session"). The
     * spy oracle is the right tool here because two-event field comparison has no in-DSL primitive
     * — `assertState` checks equality against a known value, not equality between two captures.
     */
    @Test
    fun background_long_rotates_session() {
        runScenario(rudderScenario(name = "lifecycle.background_long_rotates_session") {
            initBlock = {
                Step.Init(
                    writeKey = "test-write-key",
                    mockServerUrl = "",
                    automaticSessionTracking = true,
                    sessionTimeoutMs = SHORT_SESSION_TIMEOUT_MS,
                )
            }
            addSpyPlugin(SESSION_SPY_TAG)
            track(name = PRE_BG_EVENT)
            waitForEvent(type = StepEventType.TRACK, name = PRE_BG_EVENT)
            background()
            // Idle in background past the session timeout. assertNoEvent doubles as a sleep
            // bounded by a uniqueness predicate — the wait period is dead time for the wire.
            assertNoEvent(type = StepEventType.TRACK, name = POST_BG_EVENT, windowMs = SHORT_SESSION_TIMEOUT_MS * 2)
            foreground()
            track(name = POST_BG_EVENT)
            waitForEvent(type = StepEventType.TRACK, name = POST_BG_EVENT)
        })

        val (pre, post) = bothObservedSessionIds()
        assertNotEquals(
            "Long-background session must rotate: pre=$pre post=$post",
            pre, post,
        )
    }

    /**
     * Short backgrounding (< [LONG_SESSION_TIMEOUT_MS]) does not cross the auto-session boundary
     * — the foreground transition keeps the same sessionId. The symmetric companion to
     * [background_long_rotates_session]: same shape, same assertions, opposite expectation.
     */
    @Test
    fun background_short_keeps_session() {
        runScenario(rudderScenario(name = "lifecycle.background_short_keeps_session") {
            initBlock = {
                Step.Init(
                    writeKey = "test-write-key",
                    mockServerUrl = "",
                    automaticSessionTracking = true,
                    sessionTimeoutMs = LONG_SESSION_TIMEOUT_MS,
                )
            }
            addSpyPlugin(SESSION_SPY_TAG)
            track(name = PRE_BG_EVENT)
            waitForEvent(type = StepEventType.TRACK, name = PRE_BG_EVENT)
            background()
            assertNoEvent(type = StepEventType.TRACK, name = POST_BG_EVENT, windowMs = SHORT_BACKGROUND_MS)
            foreground()
            track(name = POST_BG_EVENT)
            waitForEvent(type = StepEventType.TRACK, name = POST_BG_EVENT)
        })

        val (pre, post) = bothObservedSessionIds()
        assertEquals(
            "Short-background session must persist: pre=$pre post=$post",
            pre, post,
        )
    }

    /**
     * Locate the spy observations for [PRE_BG_EVENT] and [POST_BG_EVENT] under [SESSION_SPY_TAG],
     * pull each event's `context.sessionId`, and return both. Fails loudly if either observation
     * or its sessionId is missing — silent nulls would mask a regression in the spy plumbing.
     */
    private fun bothObservedSessionIds(): Pair<String, String> {
        val pre = runBlocking {
            spy.awaitObservation(tag = SESSION_SPY_TAG, predicate = { it.eventName == PRE_BG_EVENT })
        }
        val post = runBlocking {
            spy.awaitObservation(tag = SESSION_SPY_TAG, predicate = { it.eventName == POST_BG_EVENT })
        }
        val preSid = pre.sessionIdFromEventJson()
        val postSid = post.sessionIdFromEventJson()
        assertNotNull("pre-bg event missing context.sessionId in eventJson=${pre.eventJson}", preSid)
        assertNotNull("post-bg event missing context.sessionId in eventJson=${post.eventJson}", postSid)
        return preSid!! to postSid!!
    }

    /**
     * Extract `context.sessionId` from the spy's serialized event payload.
     *
     * The SDK serializes `sessionId` as a JSON number; calling `.contentOrNull` reads its
     * source token regardless of numeric type, so the comparison works on the lexical id rather
     * than fighting Long-vs-Int parsing.
     */
    private fun SpyObservation.sessionIdFromEventJson(): String? {
        val raw = eventJson ?: return null
        val root = runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return null
        val context = root["context"] as? JsonObject ?: return null
        return context["sessionId"]?.jsonPrimitive?.contentOrNull
    }

    private companion object {
        // Mirrors AndroidLifecyclePlugin's internal constants. Hardcoded here (not imported
        // from the SDK) because they live in `internal` visibility; if they ever change, the
        // test fails loudly with the SDK's actual emission visible in the diff.
        const val APPLICATION_BACKGROUNDED = "Application Backgrounded"
        const val APPLICATION_OPENED = "Application Opened"

        const val SESSION_SPY_TAG = "sessionTap"
        const val PRE_BG_EVENT = "PreBg"
        const val POST_BG_EVENT = "PostBg"

        // Tight enough that a 4s wait visibly crosses it, slack enough that the SDK's session
        // timer doesn't race with broadcast latency on a slow emulator.
        const val SHORT_SESSION_TIMEOUT_MS = 2_000L

        // Long enough that a 1s background never approaches it. The SDK's default is on the
        // order of minutes; we use a smaller value just to keep the scenario self-documenting.
        const val LONG_SESSION_TIMEOUT_MS = 60_000L

        // Background dwell for the keep-session case. Comfortably below LONG_SESSION_TIMEOUT_MS.
        const val SHORT_BACKGROUND_MS = 1_000L
    }
}
