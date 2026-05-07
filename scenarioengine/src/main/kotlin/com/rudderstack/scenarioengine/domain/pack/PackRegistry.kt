package com.rudderstack.scenarioengine.domain.pack

import com.rudderstack.scenarioengine.domain.scenario.Scenario

/**
 * Lookup over the L2 pre-built scenario library (`smoke`, `lifecycle`, `identity`, `persistence`).
 *
 * The MCP `list_scenarios` and `run_scenario` tools sit on top of this. Implementations may
 * read from the filesystem, from a compiled-in catalog, or from a remote source — the
 * Interpreter only sees this interface.
 */
interface PackRegistry {
    /** Return summaries of every scenario matching [filter]. Empty filter ⇒ everything. */
    fun list(filter: PackFilter = PackFilter()): List<ScenarioSummary>

    /** Resolve a scenario by its fully-qualified name (e.g. `"lifecycle.cold_start"`). */
    fun load(name: String): Scenario
}

/**
 * Filter passed to [PackRegistry.list].
 *
 * @param pack If non-null, restrict to one pack ("lifecycle", "identity", …).
 * @param tag If non-null, only scenarios whose [com.rudderstack.scenarioengine.domain.scenario.ScenarioMetadata.tags] contain this tag.
 * @param requires If non-null, only scenarios whose `requires` set is a subset of this — i.e. only what the current environment can run.
 */
data class PackFilter(
    val pack: String? = null,
    val tag: String? = null,
    val requires: Set<String>? = null,
)

/**
 * Lightweight projection of a [Scenario] suitable for AI-agent discovery via MCP.
 * Carries enough metadata to pick a scenario without loading the full step list.
 */
data class ScenarioSummary(
    val name: String,
    val pack: String,
    val description: String,
    val tags: Set<String>,
    val runtimeEstimateMs: Long?,
    val requires: Set<String>,
)
