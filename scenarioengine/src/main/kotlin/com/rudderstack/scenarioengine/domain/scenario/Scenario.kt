package com.rudderstack.scenarioengine.domain.scenario

import com.rudderstack.scenarioengine.domain.step.Step

/**
 * A named, ordered list of [Step]s plus metadata. The unit of authoring (DSL, MCP, packs)
 * and the unit of execution (Interpreter takes one Scenario, returns one ScenarioResult).
 */
data class Scenario(
    val name: String,
    val description: String,
    val steps: List<Step>,
    val metadata: ScenarioMetadata = ScenarioMetadata(),
)

/**
 * Discoverability and gating info attached to a [Scenario].
 *
 * @param tags Free-form labels used by [com.rudderstack.scenarioengine.domain.pack.PackRegistry] filters.
 * @param asserts Human-readable summaries of what this scenario claims to verify. Surfaced to AI agents
 *                via the MCP `list_scenarios` tool; not interpreted by the engine.
 * @param runtimeEstimateMs Rough wall-clock budget. Helps schedulers and AI agents pick fast scenarios first.
 * @param requires Capability tags (e.g. `"clock-seam"`) the scenario depends on. The interpreter
 *                 emits [StepResult.Skipped] when an environment is missing one.
 */
data class ScenarioMetadata(
    val tags: Set<String> = emptySet(),
    val asserts: List<String> = emptyList(),
    val runtimeEstimateMs: Long? = null,
    val requires: Set<String> = emptySet(),
)
