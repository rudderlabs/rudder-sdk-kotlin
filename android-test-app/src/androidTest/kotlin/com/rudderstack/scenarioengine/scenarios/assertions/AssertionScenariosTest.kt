package com.rudderstack.scenarioengine.scenarios.assertions

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rudderstack.scenarioengine.application.dsl.rudderScenario
import com.rudderstack.scenarioengine.domain.step.StateField
import com.rudderstack.scenarioengine.domain.step.StepEventType
import com.rudderstack.scenarioengine.scenarios.ScenarioRunnerTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the three assertion Step types newly wired in Step 6a:
 *  - [com.rudderstack.scenarioengine.domain.step.Step.WaitForBatch]
 *  - [com.rudderstack.scenarioengine.domain.step.Step.AssertNoEvent]
 *  - [com.rudderstack.scenarioengine.domain.step.Step.AssertState]
 *
 * `WaitForEvent` and `AssertField` are exercised by every other scenario file already; no
 * dedicated test here.
 */
@RunWith(AndroidJUnit4::class)
class AssertionScenariosTest : ScenarioRunnerTest() {

    @Test
    fun waitForBatch_resolves_when_a_batch_arrives() {
        runScenario(rudderScenario(name = "assertions.wait_for_batch") {
            track(name = "TriggerBatch")
            // No assertion on the batch contents — the WaitForBatch Step's StepResult.Ok value
            // carries the parsed batch, but the DSL does not surface it. The success criterion
            // is "resolved within the timeout"; the StepResult.Failed path would have thrown.
            waitForBatch(timeoutMs = 5_000)
        })
    }

    @Test
    fun assertNoEvent_passes_when_nothing_arrives_in_window() {
        // Identify (an IDENTIFY event on the wire) — assert no TRACK event of an arbitrary
        // name arrives in the window. The IDENTIFY does flush, but its `type` is "identify",
        // not "track", so the type filter rejects it; this is the cleanest way to prove
        // assertNoEvent's filtering works.
        runScenario(rudderScenario(name = "assertions.assert_no_event") {
            identify(userId = "u1")
            assertNoEvent(type = StepEventType.TRACK, name = "Anything", windowMs = 1_000)
        })
    }

    @Test
    fun assertState_reads_userId_after_identify() {
        runScenario(rudderScenario(name = "assertions.assert_state.user_id") {
            identify(userId = "user-state")
            assertState(field = StateField.USER_ID, expected = "user-state")
        })
    }

    // [StateField.SESSION_ID] is exercised by SessionScenariosTest (both the start and the
    // post-endSession-null cases). [StateField.ANONYMOUS_ID] has no public injection seam
    // for a deterministic value, so an exact-match AssertState is impossible — its presence
    // is covered at the probe layer by ContentProviderStateProbeTest.
}
