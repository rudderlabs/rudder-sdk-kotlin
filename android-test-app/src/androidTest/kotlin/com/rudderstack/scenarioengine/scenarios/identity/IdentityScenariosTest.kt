package com.rudderstack.scenarioengine.scenarios.identity

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rudderstack.scenarioengine.application.dsl.rudderScenario
import com.rudderstack.scenarioengine.domain.step.StateField
import com.rudderstack.scenarioengine.domain.step.Step
import com.rudderstack.scenarioengine.domain.step.StepEventType
import com.rudderstack.scenarioengine.scenarios.ScenarioRunnerTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import org.junit.runner.RunWith

/**
 * L2 identity scenarios. Each composes multiple Step types into a behaviorally-meaningful flow:
 * propagation onto subsequent events, persistence across destructive lifecycle ops, partial
 * resets that surface the four-flag `Reset` contract.
 *
 * **Why a separate pack.** Identity behaviors are orthogonal to lifecycle / events / sessions —
 * they cut across all three. Grouping them into their own file makes the failure surface
 * legible: a regression in `Reset`'s flag handling, an `Alias` chain bug, or an `Identify`
 * persistence gap each lights up its own test class instead of bleeding into a generic bucket.
 *
 * **Cross-event field comparison.** Where the assertion needs a value carried by an event on
 * the wire, the pattern is `waitForEvent(type, name)` followed by `assertField(path, expected)`.
 * `assertField` reads the most-recent event resolved by `waitForEvent`. For state-not-on-wire
 * (e.g. "no active session"), [com.rudderstack.scenarioengine.domain.step.Step.AssertState]
 * is the right tool — it polls the SDK's in-process state via the StateProbe.
 */
@RunWith(AndroidJUnit4::class)
class IdentityScenariosTest : ScenarioRunnerTest() {

    /**
     * After `identify(u1)`, every subsequent event carries `userId == "u1"` at the event root.
     * The IDENTIFY event itself flushes first; the assertion is on the TRACK that follows.
     */
    @Test
    fun identify_attaches_user_id_to_subsequent_track() {
        runScenario(rudderScenario(name = "identity.identify_attaches_user_id_to_subsequent_track") {
            identify(userId = USER_A)
            track(name = TRACK_NAME)
            waitForEvent(type = StepEventType.TRACK, name = TRACK_NAME)
            assertField(path = "userId", expected = JsonPrimitive(USER_A))
        })
    }

    /**
     * **§18 doc-named L2 scenario** ("anonymous_to_identified_propagation_across_lifecycle").
     *
     * Track an event while anonymous. Identify the user. Kill the SUT process. Cold-start a
     * fresh process and re-init the SDK with no Reset (so the SDK rehydrates persisted identity
     * from disk). The follow-up Track must carry the original `userId` — proof that identity
     * persistence rides through a process death.
     */
    @Test
    fun anonymous_to_identified_propagates_across_kill() {
        runScenario(rudderScenario(name = "identity.anonymous_to_identified_propagates_across_kill") {
            track(name = "Anonymous")
            waitForEvent(type = StepEventType.TRACK, name = "Anonymous")
            identify(userId = USER_A)
            assertState(field = StateField.USER_ID, expected = USER_A)
            kill()
            coldStart()
            // Re-init *without* a Reset — the SDK should rehydrate the identified user from
            // the persisted store. This is the load-bearing assertion: a Reset here would make
            // the test pass for the wrong reason (a fresh anonymous identity on disk).
            step(Step.Init(writeKey = "test-write-key", mockServerUrl = ""))
            assertState(field = StateField.USER_ID, expected = USER_A)
            track(name = "Identified")
            waitForEvent(type = StepEventType.TRACK, name = "Identified")
            assertField(path = "userId", expected = JsonPrimitive(USER_A))
        })
    }

    /**
     * **§18 doc-named L2 scenario** ("alias_chain_then_reset").
     *
     * Walk an alias chain `A → B → C`, observe `userId == "C"` on a follow-up Track, then full
     * Reset. After Reset the SDK must report no active userId — the chain is fully torn down,
     * not just the most recent link.
     */
    @Test
    fun alias_chain_then_reset_clears_user_id() {
        runScenario(rudderScenario(name = "identity.alias_chain_then_reset_clears_user_id") {
            identify(userId = USER_A)
            // alias's previousId defaults to the current userId — the SDK auto-resolves the
            // chain. Letting the SDK pick is the canonical authoring shape; passing it
            // explicitly is what `events.alias` covers, so don't duplicate.
            alias(newId = USER_B)
            alias(newId = USER_C)
            assertState(field = StateField.USER_ID, expected = USER_C)
            track(name = TRACK_NAME)
            waitForEvent(type = StepEventType.TRACK, name = TRACK_NAME)
            assertField(path = "userId", expected = JsonPrimitive(USER_C))
            reset()
            // The SDK's StateProbe returns the empty string for a cleared userId (not null) —
            // a deliberate distinction from SESSION_ID, which goes null on `endSession`. Tests
            // need to mirror what the probe actually emits or assertState fails on the literal
            // value comparison even though the user is conceptually "gone."
            assertState(field = StateField.USER_ID, expected = CLEARED_USER_ID)
        })
    }

    /**
     * Full Reset clears userId. Symmetric with [reset_session_only_keeps_user_id]: same start
     * state, opposite Reset configuration, opposite expectation. Together they document the
     * four-flag Reset contract from both sides.
     */
    @Test
    fun identify_then_reset_clears_user_id() {
        runScenario(rudderScenario(name = "identity.identify_then_reset_clears_user_id") {
            identify(userId = USER_A)
            assertState(field = StateField.USER_ID, expected = USER_A)
            reset()
            assertState(field = StateField.USER_ID, expected = CLEARED_USER_ID)
        })
    }

    /**
     * Partial Reset with only `session = true` set: the userId is preserved. Surfaces the
     * doc-deviation widening of `Reset` to four independent flags — without coverage here a
     * regression that re-couples the flags would slip through.
     *
     * **Why no SESSION_ID assertion.** The `Reset.session` flag is documented on `Step.Reset`
     * KDoc as "refresh the active session" — i.e. *rotate to a new sessionId*, not clear.
     * Asserting "session is now null" would assert the wrong contract; asserting "session is
     * a specific new value" needs a comparison primitive the DSL doesn't have at v1. The
     * userId-preservation half is what this scenario uniquely covers; the session-rotation
     * half is implicit.
     */
    @Test
    fun reset_session_only_keeps_user_id() {
        runScenario(rudderScenario(name = "identity.reset_session_only_keeps_user_id") {
            identify(userId = USER_A)
            startSession(sessionId = MANUAL_SESSION_ID)
            assertState(field = StateField.SESSION_ID, expected = MANUAL_SESSION_ID.toString())
            reset(anonymousId = false, userId = false, traits = false, session = true)
            assertState(field = StateField.USER_ID, expected = USER_A)
        })
    }

    private companion object {
        const val USER_A = "user-A"
        const val USER_B = "user-B"
        const val USER_C = "user-C"
        const val TRACK_NAME = "Tap"

        // The SDK reports a cleared userId as the empty string via the StateProbe. Distinct
        // from SESSION_ID, which reports null when no session is active. Both shapes are
        // SDK-side decisions; tests mirror the actual emission.
        const val CLEARED_USER_ID = ""

        // SDK's MIN_SESSION_ID_LENGTH is 10 — anything shorter is silently rejected. Using a
        // recognizable timestamp-shaped value keeps the assertion readable in failure output.
        const val MANUAL_SESSION_ID = 1234567890L
    }
}
