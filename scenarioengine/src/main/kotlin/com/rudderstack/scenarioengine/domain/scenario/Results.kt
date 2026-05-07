package com.rudderstack.scenarioengine.domain.scenario

/**
 * The outcome of one [com.rudderstack.scenarioengine.domain.step.Step]'s execution.
 *
 * Results are explicit values, not exceptions — the interpreter catches helper exceptions
 * and turns them into [Failed]. Tests assert on the resulting [ScenarioResult.Passed] /
 * [ScenarioResult.Failed], not on thrown exceptions.
 */
sealed class StepResult {
    /**
     * The step succeeded.
     *
     * @param value Optional payload — e.g. the event JSON returned by `WaitForEvent`, or the
     *              blob returned by `SnapshotState`. `Any?` because callers know what shape
     *              to expect from each Step type.
     */
    data class Ok(val value: Any? = null) : StepResult()

    /** The step failed. Halts the scenario; subsequent steps are not run. */
    data class Failed(val reason: String, val cause: Throwable? = null) : StepResult()

    /**
     * The step was skipped — typically because the environment is missing a capability the
     * scenario [com.rudderstack.scenarioengine.domain.scenario.ScenarioMetadata.requires]. Skipped is
     * not a failure; the scenario continues.
     */
    data class Skipped(val reason: String) : StepResult()
}

/**
 * The outcome of running a whole [Scenario].
 *
 * [Passed] is reported only when every step's result is non-[StepResult.Failed]. [Failed]
 * carries the index of the first failing step plus the partial run history, so test reports
 * can pinpoint exactly where a scenario broke.
 */
sealed class ScenarioResult {
    /** Every step completed without a [StepResult.Failed]. May still contain [StepResult.Skipped]s. */
    data class Passed(val stepResults: List<StepResult>) : ScenarioResult()

    /**
     * The scenario halted at step [failedAt]. [stepResults] contains the completed prefix
     * (length `failedAt + 1`, with the last entry being the [StepResult.Failed] that caused the halt).
     */
    data class Failed(
        val failedAt: Int,
        val stepResults: List<StepResult>,
        val reason: String,
    ) : ScenarioResult()
}
