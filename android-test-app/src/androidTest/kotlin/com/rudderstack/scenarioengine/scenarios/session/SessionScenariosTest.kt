package com.rudderstack.scenarioengine.scenarios.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rudderstack.scenarioengine.application.dsl.rudderScenario
import com.rudderstack.scenarioengine.domain.step.StateField
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
}
