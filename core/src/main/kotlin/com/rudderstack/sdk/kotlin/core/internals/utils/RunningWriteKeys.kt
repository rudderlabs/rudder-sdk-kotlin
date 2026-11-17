package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.Analytics
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * The write keys that a running instance in this process holds.
 *
 * Two instances for one write key share one store on disk, and break each other's uploads.
 * A weak reference frees the write key of an instance that the garbage collector removed.
 */
internal object RunningWriteKeys {

    private val instances = ConcurrentHashMap<String, WeakReference<Analytics>>()

    /**
     * Records [analytics] for its write key. Returns `false` when another running instance already holds it.
     */
    fun claim(analytics: Analytics): Boolean {
        var claimed = false
        instances.compute(analytics.configuration.writeKey) { _, current ->
            val holder = current?.get()
            claimed = holder == null || holder.isAnalyticsShutdown
            if (claimed) WeakReference(analytics) else current
        }
        return claimed
    }

    /**
     * Frees the write key of [analytics], when [analytics] holds it.
     */
    fun release(analytics: Analytics) {
        instances.computeIfPresent(analytics.configuration.writeKey) { _, current ->
            if (current.get() === analytics) null else current
        }
    }
}
