package com.rudderstack.testapp.spy

import com.rudderstack.scenarioengine.domain.spy.SpyObservation

/**
 * Single-method abstraction over "where do SpyPlugin observations go".
 *
 * Production implementation is [BroadcastSpySink], which serializes the observation and
 * broadcasts it to the driver. The interface exists so SpyPlugin can be unit-tested in
 * isolation against an in-memory sink without standing up the broadcast machinery.
 */
internal fun interface SpySink {

    /**
     * Hand off [observation]. Implementations must not block — `intercept` runs on the SDK's
     * event-processing path and a slow sink would back up the queue.
     */
    fun emit(observation: SpyObservation)
}
