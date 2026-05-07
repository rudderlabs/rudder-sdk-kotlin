package com.rudderstack.scenarioengine.scenarios.lifecycle

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rudderstack.scenarioengine.application.dsl.rudderScenario
import com.rudderstack.scenarioengine.domain.step.Step
import com.rudderstack.scenarioengine.domain.step.StepEventType
import com.rudderstack.scenarioengine.scenarios.ScenarioRunnerTest
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

    private companion object {
        // Mirrors AndroidLifecyclePlugin's internal constants. Hardcoded here (not imported
        // from the SDK) because they live in `internal` visibility; if they ever change, the
        // test fails loudly with the SDK's actual emission visible in the diff.
        const val APPLICATION_BACKGROUNDED = "Application Backgrounded"
        const val APPLICATION_OPENED = "Application Opened"
    }
}
