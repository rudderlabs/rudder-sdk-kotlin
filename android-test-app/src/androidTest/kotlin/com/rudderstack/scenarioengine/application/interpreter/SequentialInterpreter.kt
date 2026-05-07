package com.rudderstack.scenarioengine.application.interpreter

import com.rudderstack.scenarioengine.domain.interpreter.Interpreter
import com.rudderstack.scenarioengine.domain.scenario.Scenario
import com.rudderstack.scenarioengine.domain.scenario.ScenarioResult
import com.rudderstack.scenarioengine.domain.scenario.StepResult
import com.rudderstack.scenarioengine.domain.step.Step
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * The default [Interpreter]. Runs a scenario step-by-step, halts on the first failure.
 *
 * **Step 5 scope.** Wires only the Step types needed for `smoke.basic_track`:
 * [Step.Init], [Step.Reset], [Step.Track], [Step.WaitForEvent], [Step.AssertField]. Every
 * other branch of the dispatch [when] is a `TODO()` keyed to the step that owns it
 * (Step 6 for events / sessions / lifecycle, Step 7 for spy plugins, Step 11 for state
 * export/import, Step 12 for system state and faults). The sealed [Step] hierarchy makes
 * this exhaustive at compile time — adding a new Step subtype forces this dispatch site to
 * be updated.
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
        // ---- wired in Step 5 ----
        is Step.Init -> {
            helpers.sut.init(step)
            StepResult.Ok()
        }
        is Step.Reset -> {
            helpers.sut.reset(step)
            StepResult.Ok()
        }
        is Step.Track -> {
            helpers.sut.track(step)
            StepResult.Ok()
        }
        is Step.WaitForEvent -> {
            val event = helpers.mockPlane.nextEvent(step.type, step.name, step.timeoutMs, step.match)
            lastObservedEvent = event
            StepResult.Ok(event)
        }
        is Step.AssertField -> assertFieldOrFail(step)

        // ---- wired in Step 6 (event / session / lifecycle taxonomy) ----
        is Step.Screen,
        is Step.Identify,
        is Step.Group,
        is Step.Alias,
        Step.Flush,
        Step.Shutdown,
        is Step.StartSession,
        Step.EndSession,
        Step.Background,
        Step.Foreground,
        Step.Kill,
        Step.ForceStop,
        Step.ColdStart,
        Step.ClearAppData,
        is Step.WaitForBatch,
        is Step.AssertNoEvent,
        is Step.AssertState -> TODO("wired in build step 6: ${step::class.simpleName}")

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
}
