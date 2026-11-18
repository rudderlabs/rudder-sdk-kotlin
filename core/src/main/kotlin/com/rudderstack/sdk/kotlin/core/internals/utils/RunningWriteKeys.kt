package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.Analytics
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * The write keys that a running instance in this process holds.
 *
 * Two instances for one write key can share one store, and break each other's uploads.
 * So one write key has at most one running instance.
 * A weak reference frees the write key of an instance that the garbage collector removed.
 */
@InternalRudderApi
object RunningWriteKeys {

    private val instances = ConcurrentHashMap<String, Holder>()

    /**
     * Returns the running instance for [writeKey] after its start completes, or `null`. A wrapper SDK uses it to share a native instance.
     */
    fun find(writeKey: String): Analytics? =
        instances[writeKey]?.takeIf { it.started }?.analytics?.takeUnless { it.isAnalyticsShutdown }

    /**
     * Lets [find] return [analytics]. The most derived type of [analytics] calls it at the end of its start.
     */
    fun markStarted(analytics: Analytics) {
        instances[analytics.configuration.writeKey]?.takeIf { it.analytics === analytics }?.started = true
    }

    /**
     * Records [analytics] for its write key. Returns `false` when another running instance already holds it.
     */
    internal fun claim(analytics: Analytics): Boolean {
        var claimed = false
        instances.compute(analytics.configuration.writeKey) { _, current ->
            val holder = current?.analytics
            claimed = holder == null || holder.isAnalyticsShutdown
            if (claimed) Holder(analytics) else current
        }
        return claimed
    }

    /**
     * Frees the write key of [analytics], when [analytics] holds it.
     */
    internal fun release(analytics: Analytics) {
        instances.computeIfPresent(analytics.configuration.writeKey) { _, current ->
            if (current.analytics === analytics) null else current
        }
    }

    private class Holder(analytics: Analytics) {
        private val reference = WeakReference(analytics)
        val analytics: Analytics? get() = reference.get()

        @Volatile
        var started = false
    }
}
