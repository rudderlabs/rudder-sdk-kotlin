package com.rudderstack.scenarioengine.domain.helper

import com.rudderstack.scenarioengine.domain.step.Step

/**
 * The driver-side handle to the SUT's SDK surface.
 *
 * One method per Step that ends up calling an SDK method. Wide on purpose — the SDK's API
 * is wide, and the helper mirrors it 1:1. Splitting it would be ceremony, not architecture.
 *
 * Implementations marshal the call across IPC (broadcast on Android, HTTP-in-SUT on iOS)
 * and wait for an ack. All methods are `suspend` because every call crosses a process
 * boundary and may take observable time.
 */
interface Sut {
    /** Initialize the SDK in the SUT with the parameters carried by [step]. */
    suspend fun init(step: Step.Init)

    /** Send a Track event. */
    suspend fun track(step: Step.Track)

    /** Send a Screen event. */
    suspend fun screen(step: Step.Screen)

    /** Send an Identify event. */
    suspend fun identify(step: Step.Identify)

    /** Send a Group event. */
    suspend fun group(step: Step.Group)

    /** Send an Alias event. */
    suspend fun alias(step: Step.Alias)

    /** Force the SDK to drain its event queue. */
    suspend fun flush()

    /** Reset SDK identity state. */
    suspend fun reset(step: Step.Reset)

    /** Shut down the SDK instance in the SUT. */
    suspend fun shutdown()

    /** Start a session (manual or auto). */
    suspend fun startSession(step: Step.StartSession)

    /** End the active session. */
    suspend fun endSession()

    /** Register a test-only `SpyPlugin` in the SUT under [tag]. */
    suspend fun addSpyPlugin(tag: String)

    /** Remove a previously-added SpyPlugin. */
    suspend fun removeSpyPlugin(tag: String)

    /**
     * Capture the SDK's current state as an opaque versioned blob.
     * Format is private to the engine; see [Step.SnapshotState].
     */
    suspend fun exportState(): ByteArray

    /**
     * Apply a previously-captured state blob to the running SDK.
     * Throws on a version mismatch rather than silently coercing.
     */
    suspend fun importState(blob: ByteArray)
}
