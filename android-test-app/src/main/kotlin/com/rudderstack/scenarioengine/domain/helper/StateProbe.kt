package com.rudderstack.scenarioengine.domain.helper

/**
 * The driver-side reader for in-SDK identity state.
 *
 * Where [MockPlane] sees the wire, this sees what the SDK currently *believes* about identity.
 * On Android, implemented as a `ContentResolver.query` against the SUT's [StateField]-keyed
 * ContentProvider — synchronous and survives `am broadcast` ordering.
 *
 * Returns null when the field is unset or the SDK has been shut down.
 */
interface StateProbe {
    /** Current `analytics.anonymousId`, or null. */
    suspend fun anonymousId(): String?

    /** Current `analytics.userId`, or null/empty. */
    suspend fun userId(): String?

    /** Current `analytics.sessionId` as a string, or null when no session is active. */
    suspend fun sessionId(): String?
}
