package com.rudderstack.scenarioengine.application.interpreter

import com.rudderstack.scenarioengine.domain.interpreter.Interpreter
import com.rudderstack.scenarioengine.domain.scenario.Scenario
import com.rudderstack.scenarioengine.domain.scenario.ScenarioResult
import com.rudderstack.scenarioengine.domain.scenario.StepResult
import com.rudderstack.scenarioengine.domain.step.StateField
import com.rudderstack.scenarioengine.domain.step.Step
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * The default [Interpreter]. Runs a scenario step-by-step, halts on the first failure.
 *
 * **Step 6b scope.** With the destructive-op survival fix landed (driver-targeted
 * `<instrumentation>` in the androidTest manifest), the four destructive lifecycle Step
 * types — [Step.Kill], [Step.ForceStop], [Step.ColdStart], [Step.ClearAppData] — wire here
 * alongside the non-destructive ones from 6a. The dispatch is now exhaustive across every
 * v1-scope Step type. Spy plugins (Step 7), state export/import (Step 11), and system
 * state / faults (Step 12) remain `TODO()`d. The sealed [Step] hierarchy keeps this
 * exhaustive at compile time.
 *
 * **AssertField cache.** [Step.AssertField] is documented as asserting against the most-recent
 * event captured by the mock plane. The interpreter holds [lastObservedEvent] as per-run
 * state, updated whenever a [Step.WaitForEvent] resolves. This keeps [com.rudderstack.scenarioengine.domain.helper.MockPlane]
 * stateless w.r.t. assertion targets — a different transport (gRPC, on-device tap) can drive
 * the same MockPlane without growing a "last event" slot in the helper interface.
 *
 * The interpreter has no Android imports, no IO, no threading constructs of its own — it
 * composes helper calls.
 */
class SequentialInterpreter(private val helpers: Helpers) : Interpreter {

    /**
     * Cache of the last event resolved by a [Step.WaitForEvent], consumed by [Step.AssertField].
     * Reset on each call to [run] so scenario runs do not bleed state.
     */
    private var lastObservedEvent: JsonObject? = null

    override suspend fun run(scenario: Scenario): ScenarioResult {
        lastObservedEvent = null
        val results = mutableListOf<StepResult>()
        scenario.steps.forEachIndexed { index, step ->
            val result = runCatching { dispatch(step) }
                .fold(
                    onSuccess = { it },
                    onFailure = { StepResult.Failed(it.message ?: it::class.simpleName ?: "unknown", it) },
                )
            results += result
            if (result is StepResult.Failed) {
                return ScenarioResult.Failed(failedAt = index, stepResults = results, reason = result.reason)
            }
        }
        return ScenarioResult.Passed(results)
    }

    private suspend fun dispatch(step: Step): StepResult = when (step) {
        // ---- events ----
        is Step.Init -> { helpers.sut.init(step); StepResult.Ok() }
        is Step.Track -> { helpers.sut.track(step); StepResult.Ok() }
        is Step.Screen -> { helpers.sut.screen(step); StepResult.Ok() }
        is Step.Identify -> { helpers.sut.identify(step); StepResult.Ok() }
        is Step.Group -> { helpers.sut.group(step); StepResult.Ok() }
        is Step.Alias -> { helpers.sut.alias(step); StepResult.Ok() }
        Step.Flush -> { helpers.sut.flush(); StepResult.Ok() }
        is Step.Reset -> { helpers.sut.reset(step); StepResult.Ok() }
        Step.Shutdown -> { helpers.sut.shutdown(); StepResult.Ok() }

        // ---- sessions ----
        is Step.StartSession -> { helpers.sut.startSession(step); StepResult.Ok() }
        Step.EndSession -> { helpers.sut.endSession(); StepResult.Ok() }

        // ---- lifecycle ----
        Step.Background -> { helpers.lifecycle.background(); StepResult.Ok() }
        Step.Foreground -> { helpers.lifecycle.foreground(); StepResult.Ok() }
        Step.Kill -> { helpers.lifecycle.kill(); StepResult.Ok() }
        Step.ForceStop -> { helpers.lifecycle.forceStop(); StepResult.Ok() }
        Step.ColdStart -> { helpers.lifecycle.coldStart(); StepResult.Ok() }
        Step.ClearAppData -> { helpers.lifecycle.clearAppData(); StepResult.Ok() }

        // ---- assertions ----
        is Step.WaitForBatch -> StepResult.Ok(helpers.mockPlane.waitForBatch(step.timeoutMs))
        is Step.WaitForEvent -> {
            val event = helpers.mockPlane.nextEvent(step.type, step.name, step.timeoutMs, step.match)
            lastObservedEvent = event
            StepResult.Ok(event)
        }
        is Step.AssertField -> assertFieldOrFail(step)
        is Step.AssertNoEvent -> {
            helpers.mockPlane.assertNoEvent(step.type, step.name, step.windowMs)
            StepResult.Ok()
        }
        is Step.AssertState -> assertStateOrFail(step)

        // ---- wired in Step 7 (spy plugins) ----
        is Step.AddSpyPlugin,
        is Step.RemoveSpyPlugin -> TODO("wired in build step 7: ${step::class.simpleName}")

        // ---- wired in Step 11 (state export / import) ----
        Step.SnapshotState,
        is Step.ImportState -> TODO("wired in build step 11: ${step::class.simpleName}")

        // ---- wired in Step 12 (system state and faults) ----
        is Step.DeepLink,
        Step.NetworkOffline,
        Step.NetworkOnline,
        Step.DozeOn,
        Step.DozeOff,
        is Step.LocaleChange,
        Step.Crash,
        Step.ThrowOnMainThread,
        Step.NativeCrash -> TODO("wired in build step 12: ${step::class.simpleName}")
    }

    /**
     * Assert that the dotted-path [Step.AssertField.path] in the most-recent observed event
     * equals [Step.AssertField.expected]. Fails when no event has been observed yet, when the
     * path does not resolve, or when the resolved value is not strictly equal to the expected.
     */
    private fun assertFieldOrFail(step: Step.AssertField): StepResult {
        val event = lastObservedEvent
            ?: return StepResult.Failed(
                "AssertField('${step.path}') has no event to assert on — no preceding WaitForEvent resolved",
            )
        val actual = resolvePath(event, step.path)
            ?: return StepResult.Failed(
                "AssertField('${step.path}') did not resolve in event: $event",
            )
        return if (actual == step.expected) {
            StepResult.Ok()
        } else {
            StepResult.Failed(
                "AssertField('${step.path}') expected ${step.expected} but was $actual",
            )
        }
    }

    /**
     * Walk [json] along the dotted [path]. Returns null when any segment misses or the path
     * descends through a non-object node — callers treat null as a path-not-found failure.
     *
     * Intentionally minimal: equality on the resolved [JsonElement] covers the simple
     * `assertField` case; richer matching lives in [Step.WaitForEvent]'s `match` list.
     */
    private fun resolvePath(json: JsonElement, path: String): JsonElement? {
        var node: JsonElement = json
        for (segment in path.split('.')) {
            val obj = node as? JsonObject ?: return null
            node = obj[segment] ?: return null
        }
        return node
    }

    /**
     * Assert the live SDK state at [Step.AssertState.field] equals [Step.AssertState.expected].
     *
     * Polls the [com.rudderstack.scenarioengine.domain.helper.StateProbe] up to
     * [STATE_POLL_TIMEOUT_MS] before failing. The retry exists because SDK init is async — the
     * SDK's `analytics.anonymousId` is computed on a coroutine inside the constructor, so a
     * synchronous read immediately after the INIT ack returns null. Most reads after non-init
     * Steps are synchronous on the in-memory state and resolve on the first poll.
     */
    private suspend fun assertStateOrFail(step: Step.AssertState): StepResult {
        val deadline = System.currentTimeMillis() + STATE_POLL_TIMEOUT_MS
        var actual: String?
        do {
            actual = readState(step.field)
            if (actual == step.expected) return StepResult.Ok()
            delay(STATE_POLL_INTERVAL_MS)
        } while (System.currentTimeMillis() < deadline)
        return StepResult.Failed(
            "AssertState(${step.field}) expected ${step.expected} but was $actual after ${STATE_POLL_TIMEOUT_MS}ms",
        )
    }

    private suspend fun readState(field: StateField): String? = when (field) {
        StateField.ANONYMOUS_ID -> helpers.state.anonymousId()
        StateField.USER_ID -> helpers.state.userId()
        StateField.SESSION_ID -> helpers.state.sessionId()
    }

    private companion object {
        const val STATE_POLL_TIMEOUT_MS = 2_000L
        const val STATE_POLL_INTERVAL_MS = 100L
    }
}
