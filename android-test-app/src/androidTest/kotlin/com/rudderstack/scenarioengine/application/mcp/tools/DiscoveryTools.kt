package com.rudderstack.scenarioengine.application.mcp.tools

import com.rudderstack.scenarioengine.application.mcp.Tool
import com.rudderstack.scenarioengine.application.mcp.ToolRegistry
import com.rudderstack.scenarioengine.application.mcp.textResult
import kotlinx.serialization.json.jsonPrimitive

/**
 * One discovery tool: `rudder.list_scenarios`. Returns a flat list of `<pack>.<test>` names
 * for every JUnit `@Test` method in the engine's scenario classes.
 *
 * **Why a hardcoded class list (and not classpath walking).** Android's runtime classpath is
 * partitioned into DEX files and `Class.forName` is the only reliable lookup primitive across
 * API levels — there's no portable `Package.getClasses()`. Walking DEX directly works
 * (`dalvik.system.DexFile`) but the API has moved across API levels and is brittle. A
 * hardcoded list of known scenario classes is a v1 simplification that costs one line per
 * new scenario file and avoids the brittleness; when [com.rudderstack.scenarioengine.domain.pack.PackRegistry]
 * gets a real implementation the doc-prescribed catalog replaces this.
 *
 * **Why use the JUnit `@Test` annotation.** The scenario classes live as JUnit instrumentation
 * tests. The test methods are the unit of authoring; `<class>.<method>` is the natural
 * scenario name. We deliberately don't read the inline `name = "smoke.basic_track"` strings
 * inside each scenario — those are author-supplied and not all consistent; the class+method
 * is structurally derivable.
 */
internal fun registerDiscoveryTools(registry: ToolRegistry) {
    registry.register(
        Tool(
            name = "rudder.list_scenarios",
            description = "List all available scenarios as a JSON array of '<class>.<method>' names.",
            inputSchema = objectSchema(
                properties = mapOf(
                    "filter" to stringField("Optional substring filter applied to the name"),
                ),
            ),
            handler = { args ->
                val filter = args["filter"]?.jsonPrimitive?.content
                val all = listAllScenarios()
                val matched = if (filter.isNullOrBlank()) all else all.filter { it.contains(filter) }
                textResult(matched.joinToString(prefix = "[", postfix = "]") { "\"$it\"" })
            },
        ),
    )
}

/**
 * Reflective enumeration of `@Test` methods on the known scenario classes. Returns a stable,
 * sorted list so the output is byte-identical across runs (catalog drift in CI surfaces as
 * a real diff, not a reordering).
 */
private fun listAllScenarios(): List<String> {
    val out = mutableListOf<String>()
    for (className in KNOWN_SCENARIO_CLASSES) {
        val klass = runCatching { Class.forName(className) }.getOrNull() ?: continue
        for (method in klass.declaredMethods) {
            if (method.isAnnotationPresent(org.junit.Test::class.java)) {
                out += "${klass.simpleName}.${method.name}"
            }
        }
    }
    return out.sorted()
}

/**
 * Every scenario class the MCP catalog should report. Keep this list aligned with the
 * `scenarios/` directory tree — a `git grep -l "ScenarioRunnerTest"` is a quick way to
 * verify completeness when adding a new pack.
 */
private val KNOWN_SCENARIO_CLASSES = listOf(
    "com.rudderstack.scenarioengine.scenarios.smoke.SmokeScenarioTest",
    "com.rudderstack.scenarioengine.scenarios.events.EventScenariosTest",
    "com.rudderstack.scenarioengine.scenarios.session.SessionScenariosTest",
    "com.rudderstack.scenarioengine.scenarios.lifecycle.LifecycleNonDestructiveScenariosTest",
    "com.rudderstack.scenarioengine.scenarios.lifecycle.LifecycleDestructiveScenariosTest",
    "com.rudderstack.scenarioengine.scenarios.assertions.AssertionScenariosTest",
    "com.rudderstack.scenarioengine.scenarios.spy.SpyPluginScenarioTest",
    "com.rudderstack.scenarioengine.scenarios.identity.IdentityScenariosTest",
    "com.rudderstack.scenarioengine.scenarios.persistence.PersistenceScenariosTest",
)
