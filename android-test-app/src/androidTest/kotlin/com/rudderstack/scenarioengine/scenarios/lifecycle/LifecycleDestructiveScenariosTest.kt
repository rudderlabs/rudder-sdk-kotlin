package com.rudderstack.scenarioengine.scenarios.lifecycle

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.rudderstack.scenarioengine.application.dsl.rudderScenario
import com.rudderstack.scenarioengine.domain.step.Step
import com.rudderstack.scenarioengine.scenarios.ScenarioRunnerTest
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Adapter / scaffolding regression guard for the two destructive Step types whose customer-facing
 * SDK behavior is *not* covered elsewhere: [Step.Kill] and [Step.ForceStop]. Both are sub-100ms
 * tests that verify the adapter actually kills the SUT process — checked via an out-of-band
 * `pidof` shell read so a stale-state bug in the adapter can't pass by reading its own writes.
 *
 * **Why these specifically and not [Step.ColdStart] / [Step.ClearAppData].** The richer "the SDK
 * behaves correctly across a destructive op" assertions live in the persistence pack
 * (`PersistenceScenariosTest` — userId surviving kill / force-stop / cold-start, queued events
 * replaying after kill, clearAppData wiping identity). Those scenarios already exercise the
 * adapter calls *with* a behavior-level assertion, so a separate scaffolding-level test here
 * would be redundant. Kill and ForceStop are kept because the persistence pack uses them only
 * inside larger flows; a regression that breaks `am force-stop` itself surfaces faster as a
 * direct one-line test than as a downstream flake in a persistence scenario.
 *
 * **Why these are testable at all.** The androidTest manifest declares
 * `<instrumentation android:targetPackage="com.rudderstack.testapp.driver" tools:replace=...>`
 * so the test process runs under the driver UID, not the SUT's. AMS no longer ties the
 * instrumentation lifecycle to the SUT — `pm clear` / `am force-stop` / `am kill` on the SUT
 * affect only the SUT. The fact that *these tests pass* is itself proof of the Step 6b survival
 * fix.
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

    private fun sutIsRunning(): Boolean =
        device.executeShellCommand("pidof $SUT_PACKAGE").trim().isNotEmpty()

    private companion object {
        const val SUT_PACKAGE = "com.rudderstack.testapp"
    }
}
