package com.rudderstack.scenarioengine.application.dsl

import com.rudderstack.scenarioengine.domain.scenario.Scenario
import com.rudderstack.scenarioengine.domain.step.FieldMatch
import com.rudderstack.scenarioengine.domain.step.StateField
import com.rudderstack.scenarioengine.domain.step.Step
import com.rudderstack.scenarioengine.domain.step.StepEventType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Marker that scopes lambda receivers inside [rudderScenario] blocks. Prevents accidental
 * receiver leakage when nested DSL builders are added later (e.g. a future `withSession { }`).
 */
@DslMarker
annotation class ScenarioDsl

/**
 * Builder for scenario authoring.
 *
 * One helper method per [Step] subtype. `build()` collects the appended steps into a [Scenario].
 * The builder is the single place where author intent meets the typed Step taxonomy — adding
 * a new helper here is the open/closed extension point on the authoring side.
 *
 * **Default init.** When [defaultInit] is true (the canonical case), [build] prepends
 * `[Init, Reset]` so authors don't repeat the boilerplate. The synthesized [Step.Init]'s
 * `mockServerUrl` is left empty as a sentinel — the runner
 * ([com.rudderstack.scenarioengine.application.runner.ScenarioRunnerTest]) rewrites it with
 * the live mock server URL right before interpretation. Authors can opt out by setting
 * `initAnalytics = false` in [rudderScenario] when a scenario manages its own init lifecycle
 * (e.g. exercising re-init).
 *
 * Helpers for unwired Step types (Screen, Identify, Group, Alias, lifecycle, sessions, etc.)
 * exist deliberately — authors can reference them at compile time even though the interpreter
 * `TODO()`s the dispatch until later steps. This keeps the DSL surface stable from Step 5
 * onwards; only the interpreter changes as taxonomy expands.
 */
@ScenarioDsl
class RudderScenarioBuilder(
    private val name: String,
    private val description: String,
) {
    private val steps = mutableListOf<Step>()

    /** When true, [build] prepends `[Init, Reset]`. Authors flip via `initAnalytics` on [rudderScenario]. */
    var defaultInit: Boolean = true

    /** Optional override for the auto-prepended [Step.Init]. Useful when a scenario needs non-default Init knobs. */
    var initBlock: (() -> Step.Init)? = null

    /** Append [step] to the scenario. Authors typically use the typed helpers below. */
    fun step(step: Step) { steps += step }

    // ---- event helpers ----
    fun track(name: String, properties: JsonObject = JsonObject(emptyMap())) =
        step(Step.Track(name, properties))

    fun screen(name: String, category: String? = null, properties: JsonObject = JsonObject(emptyMap())) =
        step(Step.Screen(name, category, properties))

    fun identify(userId: String, traits: JsonObject = JsonObject(emptyMap())) =
        step(Step.Identify(userId, traits))

    fun group(groupId: String, traits: JsonObject = JsonObject(emptyMap())) =
        step(Step.Group(groupId, traits))

    fun alias(newId: String, previousId: String? = null) =
        step(Step.Alias(newId, previousId))

    fun flush() = step(Step.Flush)

    fun reset(
        anonymousId: Boolean = true,
        userId: Boolean = true,
        traits: Boolean = true,
        session: Boolean = true,
    ) = step(Step.Reset(anonymousId, userId, traits, session))

    fun shutdown() = step(Step.Shutdown)

    // ---- session ----
    fun startSession(sessionId: Long? = null) = step(Step.StartSession(sessionId))

    fun endSession() = step(Step.EndSession)

    // ---- lifecycle ----
    fun background() = step(Step.Background)

    fun foreground() = step(Step.Foreground)

    fun kill() = step(Step.Kill)

    fun forceStop() = step(Step.ForceStop)

    fun coldStart() = step(Step.ColdStart)

    fun clearAppData() = step(Step.ClearAppData)

    // ---- assertions ----
    fun waitForBatch(timeoutMs: Long = DEFAULT_TIMEOUT_MS) = step(Step.WaitForBatch(timeoutMs))

    fun waitForEvent(
        type: StepEventType,
        name: String? = null,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        match: List<FieldMatch> = emptyList(),
    ) = step(Step.WaitForEvent(type, name, timeoutMs, match))

    fun assertField(path: String, expected: JsonElement) =
        step(Step.AssertField(path, expected))

    fun assertNoEvent(type: StepEventType, name: String? = null, windowMs: Long = DEFAULT_NO_EVENT_WINDOW_MS) =
        step(Step.AssertNoEvent(type, name, windowMs))

    fun assertState(field: StateField, expected: String?) =
        step(Step.AssertState(field, expected))

    // ---- spy plugins ----
    fun addSpyPlugin(tag: String) = step(Step.AddSpyPlugin(tag))

    fun removeSpyPlugin(tag: String) = step(Step.RemoveSpyPlugin(tag))

    /**
     * Materialize the [Scenario]. Called by [rudderScenario]; not part of the authoring surface.
     */
    internal fun build(): Scenario {
        val finalSteps = buildList {
            if (defaultInit) {
                val init = initBlock?.invoke() ?: defaultInitStep()
                add(init)
                add(Step.Reset())
            }
            addAll(steps)
        }
        return Scenario(name, description, finalSteps)
    }

    private fun defaultInitStep(): Step.Init = Step.Init(
        writeKey = DEFAULT_WRITE_KEY,
        // Sentinel — the runner rewrites this to the live mock server URL right before
        // interpretation. The SUT's INIT handler rejects an empty value loud-and-clear.
        mockServerUrl = "",
    )

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 10_000L
        const val DEFAULT_NO_EVENT_WINDOW_MS = 2_000L
        const val DEFAULT_WRITE_KEY = "test-write-key"
    }
}

/**
 * Top-level entry point for scenario authoring.
 *
 * @param initAnalytics If true (default), [RudderScenarioBuilder.build] prepends `[Init, Reset]`.
 *                      Set to false for scenarios that manage their own init lifecycle.
 */
fun rudderScenario(
    name: String,
    description: String = "",
    initAnalytics: Boolean = true,
    block: RudderScenarioBuilder.() -> Unit,
): Scenario {
    val builder = RudderScenarioBuilder(name, description).apply { defaultInit = initAnalytics }
    builder.block()
    return builder.build()
}
