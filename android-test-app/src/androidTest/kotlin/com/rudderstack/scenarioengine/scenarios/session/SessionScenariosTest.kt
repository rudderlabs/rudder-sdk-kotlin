package com.rudderstack.scenarioengine.scenarios.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rudderstack.scenarioengine.application.dsl.rudderScenario
import com.rudderstack.scenarioengine.domain.step.StateField
import com.rudderstack.scenarioengine.domain.step.StepEventType
import com.rudderstack.scenarioengine.scenarios.ScenarioRunnerTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Session control: [com.rudderstack.scenarioengine.domain.step.Step.StartSession] and
 * [com.rudderstack.scenarioengine.domain.step.Step.EndSession].
 *
 * The Reset auto-prepended by `defaultInit = true` carries `session = true`, so each scenario
 * starts with no active session. That's the SDK's natural "manual session mode, nothing
 * started yet" state — the right baseline for these tests.
 */
@RunWith(AndroidJUnit4::class)
class SessionScenariosTest : ScenarioRunnerTest() {

    @Test
    fun startSession_with_explicit_id_is_visible_via_state_probe() {
        runScenario(rudderScenario(name = "session.start_explicit") {
            // Session IDs <10 digits are silently rejected by the SDK
            // (Analytics.MIN_SESSION_ID_LENGTH). Using a real timestamp-shaped 10-digit value.
            startSession(sessionId = 1234567890L)
            assertState(field = StateField.SESSION_ID, expected = "1234567890")
        })
    }

    @Test
    fun endSession_clears_active_session_id() {
        runScenario(rudderScenario(name = "session.end") {
            startSession(sessionId = 9876543210L)
            assertState(field = StateField.SESSION_ID, expected = "9876543210")
            endSession()
            assertState(field = StateField.SESSION_ID, expected = null)
        })
    }

    /**
     * Track during an active manual session, end the session, track again. The first track
     * emits inside the session; the second emits with no active session. The on-wire payload
     * shape isn't directly asserted (no helper for "field is absent") — instead the in-SDK
     * state probe is the oracle, and the second track is there to prove `endSession` doesn't
     * choke event emission.
     */
    @Test
    fun end_then_track_clears_session_id() {
        runScenario(rudderScenario(name = "session.end_then_track_clears_session_id") {
            startSession(sessionId = SESSION_ONE)
            assertState(field = StateField.SESSION_ID, expected = SESSION_ONE.toString())
            track(name = "InsideSession")
            waitForEvent(type = StepEventType.TRACK, name = "InsideSession")
            endSession()
            assertState(field = StateField.SESSION_ID, expected = null)
            track(name = "OutsideSession")
            waitForEvent(type = StepEventType.TRACK, name = "OutsideSession")
        })
    }

    /**
     * Walk a session lifecycle through start → end → start with two distinct manual ids. The
     * second `startSession` must take effect (overriding the previous nulled-out state) — a
     * regression where `endSession` left lingering state could let a stale id leak back, which
     * `assertState` against the new id would catch.
     */
    @Test
    fun start_then_end_then_start_creates_new_session() {
        runScenario(rudderScenario(name = "session.start_then_end_then_start_creates_new_session") {
            startSession(sessionId = SESSION_ONE)
            assertState(field = StateField.SESSION_ID, expected = SESSION_ONE.toString())
            endSession()
            assertState(field = StateField.SESSION_ID, expected = null)
            startSession(sessionId = SESSION_TWO)
            assertState(field = StateField.SESSION_ID, expected = SESSION_TWO.toString())
        })
    }

    /**
     * Manual sessions are not subject to `sessionTimeoutMs` — they persist until an explicit
     * `endSession`. Verify the contract: a short background, then foreground, then read state;
     * the manual sessionId remains unchanged. The companion to the auto-session pair in
     * [com.rudderstack.scenarioengine.scenarios.lifecycle.LifecycleNonDestructiveScenariosTest]:
     * those test auto-session rotation, this tests manual-session immunity.
     */
    @Test
    fun short_background_does_not_advance_manual_session() {
        runScenario(rudderScenario(name = "session.short_background_does_not_advance_manual_session") {
            startSession(sessionId = SESSION_ONE)
            assertState(field = StateField.SESSION_ID, expected = SESSION_ONE.toString())
            background()
            // No event should fire during the dwell — automaticSessionTracking is off (default)
            // so the SDK doesn't emit lifecycle tracks. A short window is enough to prove the
            // adapter completed the bg transition before the next step runs.
            assertNoEvent(type = StepEventType.TRACK, name = "Anything", windowMs = 500)
            foreground()
            assertState(field = StateField.SESSION_ID, expected = SESSION_ONE.toString())
        })
    }

    private companion object {
        // Both ids satisfy the SDK's MIN_SESSION_ID_LENGTH = 10 constraint and are visually
        // distinct so a swap or stale-state regression is obvious in failure output.
        const val SESSION_ONE = 1111111111L
        const val SESSION_TWO = 2222222222L
    }
}
