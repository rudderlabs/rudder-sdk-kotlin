package com.rudderstack.scenarioengine.application.interpreter

import com.rudderstack.scenarioengine.domain.helper.LifecycleControl
import com.rudderstack.scenarioengine.domain.helper.MockPlane
import com.rudderstack.scenarioengine.domain.helper.StateProbe
import com.rudderstack.scenarioengine.domain.helper.Sut

/**
 * The four helpers the [SequentialInterpreter] composes.
 *
 * Plain DI bag — one slot per category in the doc's helper taxonomy (§4.4). The runner builds
 * one per scenario from concrete adapters; the interpreter doesn't see Android types.
 *
 * @param sut Driver-side handle to the SDK's API surface.
 * @param lifecycle Driver-side knob for SUT process / activity lifecycle.
 * @param mockPlane The wire-side oracle: what did the SDK send over HTTP.
 * @param state The in-SDK identity probe: what does the SDK believe about anonymousId / userId / sessionId.
 */
data class Helpers(
    val sut: Sut,
    val lifecycle: LifecycleControl,
    val mockPlane: MockPlane,
    val state: StateProbe,
)
