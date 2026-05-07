package com.rudderstack.scenarioengine.scenarios.smoke

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rudderstack.scenarioengine.application.dsl.rudderScenario
import com.rudderstack.scenarioengine.domain.step.StepEventType
import com.rudderstack.scenarioengine.scenarios.ScenarioRunnerTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Step 5 smoke scenario: a Track event reaches the data plane with the correct payload.
 *
 * Exercises the full end-to-end path:
 *  - DSL builder (`rudderScenario` + `track` + `waitForEvent` + `assertField`)
 *  - DSL auto-init: the builder prepends `[Init, Reset]`
 *  - Runner injects the live mock server URL into Init.mockServerUrl
 *  - Interpreter dispatches Init → BroadcastSut → BroadcastTransport → SUT.Dispatcher → SDK.init
 *  - Same path for Reset and Track
 *  - SDK flushes (flushAt=1) → POST /v1/batch lands at OkHttpMockServer
 *  - OkHttpMockPlane parses the batch, matches type=track / event=Purchase
 *  - AssertField walks the dotted path and compares JsonElement equality
 *
 * If this passes, the scaffolding works. If it fails, the failure surfaces the exact step
 * via [com.rudderstack.scenarioengine.domain.scenario.ScenarioResult.Failed].
 */
@RunWith(AndroidJUnit4::class)
class SmokeScenarioTest : ScenarioRunnerTest() {

    @Test
    fun basic_track() {
        val scenario = rudderScenario(
            name = "smoke.basic_track",
            description = "track event reaches the data plane with correct payload",
        ) {
            track(
                name = "Purchase",
                properties = kotlinx.serialization.json.buildJsonObject {
                    put("amount", JsonPrimitive(99))
                    put("currency", JsonPrimitive("USD"))
                },
            )
            waitForEvent(type = StepEventType.TRACK, name = "Purchase")
            assertField(path = "properties.amount", expected = JsonPrimitive(99))
        }
        runScenario(scenario)
    }
}
