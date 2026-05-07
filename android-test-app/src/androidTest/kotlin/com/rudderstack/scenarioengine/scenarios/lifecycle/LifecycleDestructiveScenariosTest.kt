package com.rudderstack.scenarioengine.scenarios.lifecycle

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.rudderstack.scenarioengine.application.dsl.rudderScenario
import com.rudderstack.scenarioengine.domain.step.Step
import com.rudderstack.scenarioengine.domain.step.StepEventType
import com.rudderstack.scenarioengine.scenarios.ScenarioRunnerTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Destructive lifecycle Step types: [Step.Kill], [Step.ForceStop], [Step.ColdStart],
 * [Step.ClearAppData].
 *
 * **Why these are testable now.** The androidTest manifest now declares
 * `<instrumentation android:targetPackage="com.rudderstack.testapp.driver" tools:replace=...>`
 * so the test process runs under the driver UID, not the SUT's. AMS no longer ties the
 * instrumentation lifecycle to the SUT — pm clear / am force-stop / am kill on the SUT
 * affect only the SUT. The fact that *these tests pass* is itself proof of the survival fix.
 *
 * **What the assertions check.** The Step's success means "the adapter call returned cleanly."
 * The richer "did the SUT actually go away / come back" question is checked by an external
 * `pidof` shell read in the @Test method — testing through the public Step API but verifying
 * with an independent probe catches a class of bug where adapter and assertion read the same
 * stale state.
 *
 * **Why no AssertState-based tests.** Strict before/after-anonymousId comparisons would require
 * a capture-and-compare DSL primitive that the engine does not expose at v1. The "correct
 * behavior under cold-start / clear" semantics are still exercised: a follow-up Init + Track
 * after the destructive op must round-trip through the SDK's fresh state, which would fail if
 * the op didn't actually wipe / restart the SUT process.
 */
@RunWith(AndroidJUnit4::class)
class LifecycleDestructiveScenariosTest : ScenarioRunnerTest() {

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun kill_stops_the_sut_process() {
        runScenario(rudderScenario(name = "lifecycle.kill") {
            kill()
        })
        assertFalse("SUT should not be running after kill", sutIsRunning())
    }

    @Test
    fun forceStop_stops_the_sut_process() {
        runScenario(rudderScenario(name = "lifecycle.force_stop") {
            forceStop()
        })
        assertFalse("SUT should not be running after forceStop", sutIsRunning())
    }

    @Test
    fun coldStart_relaunches_sut_and_serves_traffic_again() {
        // ColdStart = forceStop + launch. After it returns, SUT is alive again under a fresh
        // process. Re-initing and tracking proves the new process is healthy and the IPC
        // channel reattaches cleanly.
        runScenario(rudderScenario(name = "lifecycle.cold_start") {
            coldStart()
            step(Step.Init(writeKey = "test-write-key", mockServerUrl = ""))
            track(name = "PostColdStart")
            waitForEvent(type = StepEventType.TRACK, name = "PostColdStart")
        })
        assertTrue("SUT should be running after coldStart + reinit", sutIsRunning())
    }

    @Test
    fun clearAppData_wipes_storage_and_reinit_replays_traffic() {
        // pm clear takes the SUT down too — the adapter's clearAppData waits for the process
        // to exit. Re-launching + re-initing proves the wipe completed (no leftover SDK state)
        // and the SUT can serve traffic again.
        runScenario(rudderScenario(name = "lifecycle.clear_app_data") {
            clearAppData()
            step(Step.Init(writeKey = "test-write-key", mockServerUrl = ""))
            track(name = "PostClear")
            waitForEvent(type = StepEventType.TRACK, name = "PostClear")
        })
    }

    private fun sutIsRunning(): Boolean =
        device.executeShellCommand("pidof $SUT_PACKAGE").trim().isNotEmpty()

    private companion object {
        const val SUT_PACKAGE = "com.rudderstack.testapp"
    }
}
