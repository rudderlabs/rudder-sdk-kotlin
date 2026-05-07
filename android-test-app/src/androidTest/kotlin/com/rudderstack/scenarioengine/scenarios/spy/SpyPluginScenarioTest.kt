package com.rudderstack.scenarioengine.scenarios.spy

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rudderstack.scenarioengine.application.dsl.rudderScenario
import com.rudderstack.scenarioengine.domain.step.StepEventType
import com.rudderstack.scenarioengine.scenarios.ScenarioRunnerTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end exercise of the Step 7 SpyPlugin path.
 *
 * The two scenarios cover the contract from both sides:
 *
 *  1. **Add → track → assert.** The SpyPlugin registered under [SPY_TAG] must observe the
 *     subsequent track event. This is the canonical "two-oracle" use case from §12.3 — the
 *     mock plane confirms wire emission, the spy confirms in-process interception.
 *
 *  2. **Remove → track → no observation.** After a spy is removed, further events must not
 *     appear in its observation stream. Covers the deregistration half of the contract.
 *
 * The tests use `runBlocking` to bridge to [com.rudderstack.scenarioengine.domain.helper.SpyOracle]'s
 * suspend API. The oracle's eager subscription (in `BroadcastSpyOracle.init`) means observations
 * emitted between the scenario's `track` step and the post-scenario assertion are captured —
 * authors don't need to register a listener before triggering the SDK action.
 */
@RunWith(AndroidJUnit4::class)
class SpyPluginScenarioTest : ScenarioRunnerTest() {

    @Test
    fun spy_observes_intercepted_track_event() {
        runScenario(rudderScenario(name = "spy.observes_track") {
            addSpyPlugin(SPY_TAG)
            track(name = TRACK_NAME)
        })

        val observation = runBlocking {
            spy.awaitObservation(
                tag = SPY_TAG,
                predicate = { it.eventType == "track" && it.eventName == TRACK_NAME },
            )
        }
        assertEquals("intercepted", observation.kind)
        assertEquals("track", observation.eventType)
        assertEquals(TRACK_NAME, observation.eventName)
        assertTrue(
            "Spy observation should carry the serialized event JSON for richer assertions",
            !observation.eventJson.isNullOrBlank(),
        )
    }

    @Test
    fun removed_spy_does_not_observe_subsequent_events() {
        // The scenario waits for "AfterRemove" on the wire. Once the mock plane has the event,
        // the SDK's plugin chain has finished processing it — i.e. if the (now-removed) spy
        // were ever going to see it, it would have already. This pattern avoids a brittle
        // Thread.sleep and ties the negative assertion to a positive observation upstream.
        runScenario(rudderScenario(name = "spy.remove_stops_observation") {
            addSpyPlugin(SPY_TAG)
            track(name = "BeforeRemove")
            removeSpyPlugin(SPY_TAG)
            track(name = "AfterRemove")
            waitForEvent(type = StepEventType.TRACK, name = "AfterRemove")
        })

        val seen = spy.observations(SPY_TAG).map { it.eventName }
        assertTrue(
            "Spy should have observed 'BeforeRemove' track: actual=$seen",
            seen.contains("BeforeRemove"),
        )
        assertTrue(
            "Spy must not observe events sent after RemoveSpyPlugin: actual=$seen",
            seen.none { it == "AfterRemove" },
        )
    }

    private companion object {
        const val SPY_TAG = "trackTap"
        const val TRACK_NAME = "Tap"
    }
}
