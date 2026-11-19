package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.Analytics
import java.lang.ref.WeakReference

/**
 * The write keys that a running instance in this process holds.
 *
 * Two instances for one write key can share one store, and break each other's uploads.
 * So one write key has at most one running instance.
 * A weak reference frees the write key of an instance that the garbage collector removed.
 */
@InternalRudderApi
object RunningWriteKeys {

    // A plain map under one lock, because the atomic ConcurrentHashMap operations need Android API 24.
    private val instances = HashMap<String, Holder>()

    /**
     * Returns the running instance for [writeKey] after its start completes, or `null`. A wrapper SDK uses it to share a native instance.
     */
    fun find(writeKey: String): Analytics? = synchronized(instances) {
        instances[writeKey]?.takeIf { it.started }?.analytics?.takeUnless { it.isAnalyticsShutdown }
    }

    /**
     * Lets [find] return [analytics]. The most derived type of [analytics] calls it at the end of its start.
     */
    fun markStarted(analytics: Analytics) {
        synchronized(instances) {
            instances[analytics.configuration.writeKey]?.takeIf { it.analytics === analytics }?.started = true
        }
    }

    /**
     * Records [analytics] for its write key. Returns `false` when another running instance already holds it.
     */
    internal fun claim(analytics: Analytics): Boolean = synchronized(instances) {
        val writeKey = analytics.configuration.writeKey
        val holder = instances[writeKey]?.analytics
        val claimed = holder == null || holder.isAnalyticsShutdown
        if (claimed) instances[writeKey] = Holder(analytics)
        claimed
    }

    /**
     * Frees the write key of [analytics], when [analytics] holds it.
     */
    internal fun release(analytics: Analytics) {
        synchronized(instances) {
            val writeKey = analytics.configuration.writeKey
            if (instances[writeKey]?.analytics === analytics) instances.remove(writeKey)
        }
    }

    private class Holder(analytics: Analytics) {
        private val reference = WeakReference(analytics)
        val analytics: Analytics? get() = reference.get()
        var started = false
    }
}
