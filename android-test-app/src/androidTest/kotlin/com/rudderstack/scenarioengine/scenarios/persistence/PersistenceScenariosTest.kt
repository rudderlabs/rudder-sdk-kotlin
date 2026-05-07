package com.rudderstack.scenarioengine.scenarios.persistence

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
 * L2 persistence scenarios. Each asserts on what the SDK preserves (or correctly forgets)
 * across a process boundary — `kill`, `forceStop`, `clearAppData`. The SUT process dies; the
 * test re-inits the SDK on the new process and observes whether on-disk state survived.
 *
 * **What this pack proves vs. lifecycle.** The lifecycle pack proves a destructive op *runs
 * cleanly* — the SUT goes away, comes back, IPC reattaches. This pack proves *the SDK preserves
 * the right things across that transition*: identity persistence, queued-event persistence,
 * and clear-data wiping. They share the same destructive-op machinery; the assertion target
 * is what's different.
 *
 * **No `NetworkOffline` scenarios.** The doc's example
 * `persistence.offline_queue_replay_then_partial_5xx` uses Step 12's `NetworkOffline` /
 * `NetworkOnline`, which aren't wired yet. The closest in-scope flow is
 * [queued_events_replay_after_kill] — buffer events at the SDK by configuring `flushAt = 100`,
 * kill mid-flight, observe replay on cold-start. Same persistence-of-queued-events question,
 * different mechanism for the buffering.
 */
@RunWith(AndroidJUnit4::class)
class PersistenceScenariosTest : ScenarioRunnerTest() {

    /**
     * Identified userId survives `kill` + `coldStart`. The SDK persists user identity to disk;
     * a fresh process must rehydrate it on init without a Reset.
     */
    @Test
    fun user_id_survives_kill_and_cold_start() {
        runScenario(rudderScenario(name = "persistence.user_id_survives_kill_and_cold_start") {
            identify(userId = USER_A)
            assertState(field = StateField.USER_ID, expected = USER_A)
            kill()
            coldStart()
            // Reset is *not* prepended here — a Reset would clear the on-disk identity we're
            // testing for. The cold-start Init reads what's persisted and the assertion checks
            // it's still the value we identified pre-kill.
            step(Step.Init(writeKey = "test-write-key", mockServerUrl = ""))
            assertState(field = StateField.USER_ID, expected = USER_A)
            track(name = "PostKill")
            waitForEvent(type = StepEventType.TRACK, name = "PostKill")
            assertField(path = "userId", expected = JsonPrimitive(USER_A))
        })
    }

    /**
     * **§18 doc-flavored L2 scenario** (the in-scope variant of
     * `persistence.offline_queue_replay_then_partial_5xx`).
     *
     * Buffer one identified event at the SDK with `flushAt = 100`, kill mid-flight, cold-start,
     * re-init with `flushAt = 1`. The persisted event must drain onto the wire on the new
     * process — and carry the original userId, proving identity-and-event persistence are
     * coupled correctly across process death.
     *
     * **Why one event, not three.** The mock plane consumes one *batch* per `waitForEvent` call
     * (see `OkHttpMockPlane.nextEvent`). When a kill+cold-start replay drains the persisted
     * queue, all queued events typically arrive in a single batch — a sequence of `waitForEvent`
     * calls would match the first and time out on the rest. Multi-event batch assertions need
     * a `waitForBatch` + size check primitive that the DSL doesn't surface to authors at v1;
     * scenarios validate one event-survival per case and trust the mock plane's per-batch
     * filtering for exactness.
     */
    @Test
    fun queued_events_replay_after_kill() {
        runScenario(rudderScenario(name = "persistence.queued_events_replay_after_kill") {
            initBlock = {
                Step.Init(
                    writeKey = "test-write-key",
                    mockServerUrl = "",
                    flushAt = HIGH_FLUSH_AT,
                )
            }
            identify(userId = USER_A)
            track(name = QUEUED_EVENT)
            // No Flush — the event stays in the SDK queue. Confirm it didn't reach the wire,
            // otherwise the rest of the scenario would prove nothing about persistence.
            assertNoEvent(type = StepEventType.TRACK, name = QUEUED_EVENT, windowMs = 500)
            kill()
            coldStart()
            // Re-init with flushAt = 1 so the SDK is configured to flush on each event going
            // forward. Init alone is insufficient — the persisted queue does not auto-drain
            // on init in observed runs; the queue replay needs an explicit drain trigger.
            step(Step.Init(writeKey = "test-write-key", mockServerUrl = "", flushAt = 1))
            // flush() drives the persisted queue onto the wire on the new process. This is
            // the load-bearing call that turns "events sit on disk" into "events arrive at
            // the mock server"; remove it and the test times out waiting forever.
            flush()
            waitForEvent(type = StepEventType.TRACK, name = QUEUED_EVENT, timeoutMs = LONG_TIMEOUT_MS)
            // userId on the persisted event is what catches a regression that drops only the
            // identity envelope across persistence — the event survives but loses its user.
            assertField(path = "userId", expected = JsonPrimitive(USER_A))
        })
    }

    /**
     * `forceStop` is a softer process death than `kill` (no SIGKILL semantics on the SDK's
     * coroutines), but the persistence contract is the same — userId survives onto disk and
     * the cold-started process rehydrates.
     */
    @Test
    fun user_id_persists_through_force_stop() {
        runScenario(rudderScenario(name = "persistence.user_id_persists_through_force_stop") {
            identify(userId = USER_A)
            assertState(field = StateField.USER_ID, expected = USER_A)
            forceStop()
            coldStart()
            step(Step.Init(writeKey = "test-write-key", mockServerUrl = ""))
            assertState(field = StateField.USER_ID, expected = USER_A)
            track(name = "PostForceStop")
            waitForEvent(type = StepEventType.TRACK, name = "PostForceStop")
            assertField(path = "userId", expected = JsonPrimitive(USER_A))
        })
    }

    /**
     * `clearAppData` wipes the SUT's `/data/data/<pkg>/` tree. After the wipe and a fresh init,
     * the SDK starts blank — no persisted userId, no queued events. The complement to the three
     * "userId survives X" scenarios above: this one proves the SDK *does not* preserve identity
     * across an explicit clear.
     */
    @Test
    fun user_id_cleared_by_clear_app_data() {
        runScenario(rudderScenario(name = "persistence.user_id_cleared_by_clear_app_data") {
            identify(userId = USER_A)
            assertState(field = StateField.USER_ID, expected = USER_A)
            clearAppData()
            // clearAppData implicitly takes the SUT down. Re-init on the wiped data dir.
            step(Step.Init(writeKey = "test-write-key", mockServerUrl = ""))
            // Cleared userId surfaces as the empty string from the StateProbe, not null —
            // see the matching note in IdentityScenariosTest.CLEARED_USER_ID.
            assertState(field = StateField.USER_ID, expected = CLEARED_USER_ID)
        })
    }

    private companion object {
        const val USER_A = "persisted-user"
        const val QUEUED_EVENT = "QueuedAcrossKill"

        // The SDK reports a cleared userId as the empty string (not null) via the StateProbe.
        const val CLEARED_USER_ID = ""

        // High enough that quick tracks don't cross any default flush policy. The SDK's default
        // CountFlushPolicy threshold is 30; 100 is well above that ceiling.
        const val HIGH_FLUSH_AT = 100

        // Replay across a cold start can take noticeably longer than a within-process flush —
        // the SDK has to read the persisted queue, re-batch, and POST. Generous timeout to
        // avoid emulator-flakiness false negatives.
        const val LONG_TIMEOUT_MS = 20_000L
    }
}
