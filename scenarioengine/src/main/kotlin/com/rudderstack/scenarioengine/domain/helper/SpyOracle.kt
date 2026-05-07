package com.rudderstack.scenarioengine.domain.helper

import com.rudderstack.scenarioengine.domain.spy.SpyObservation

/**
 * Driver-side handle to the stream of [SpyObservation]s emitted by SUT-side spy plugins.
 *
 * The companion to [com.rudderstack.scenarioengine.domain.helper.MockPlane]: where MockPlane
 * answers *"what did the SDK send over the wire?"*, [SpyOracle] answers *"what did the SDK
 * do internally?"* (§12.3 of the design doc).
 *
 * Implementations are expected to subscribe to the SUT's `EVENT_TYPE_SDK_EVENT` broadcasts
 * eagerly — observations emitted before [awaitObservation] is called must still be matchable.
 * See the doc on [awaitObservation] for the buffering contract.
 */
interface SpyOracle {

    /**
     * Suspend until an observation matching [predicate] has been seen for [tag], or fail
     * with a timeout error after [timeoutMs].
     *
     * Resolves immediately if a matching observation has already arrived — callers do not
     * need to register a listener before triggering the SDK action.
     *
     * @param tag The tag the spy was registered under.
     * @param predicate Optional refinement on top of the tag match. Defaults to "any observation".
     * @param timeoutMs Maximum wall-clock time to wait.
     */
    suspend fun awaitObservation(
        tag: String,
        predicate: (SpyObservation) -> Boolean = { true },
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ): SpyObservation

    /** Snapshot of every observation buffered for [tag] so far. May be empty. */
    fun observations(tag: String): List<SpyObservation>

    companion object {
        const val DEFAULT_TIMEOUT_MS = 5_000L
    }
}
