package com.rudderstack.scenarioengine.domain.interpreter

import com.rudderstack.scenarioengine.domain.scenario.Scenario
import com.rudderstack.scenarioengine.domain.scenario.ScenarioResult

/**
 * Executes a [Scenario] and returns its outcome.
 *
 * One method, one verb. Implementations may be sequential, parallel, retry-aware, time-warping —
 * none of that leaks into the contract. The Interpreter composes helper calls; it is the only
 * place where Step-types meet the helper interfaces, and the only place where the dispatch
 * `when` over the sealed [com.rudderstack.scenarioengine.domain.step.Step] lives.
 */
interface Interpreter {
    /**
     * Run [scenario] to completion (success) or first failure. Never throws —
     * helper exceptions are caught and reported as [com.rudderstack.scenarioengine.domain.scenario.StepResult.Failed].
     */
    suspend fun run(scenario: Scenario): ScenarioResult
}
