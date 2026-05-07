package com.rudderstack.testapp.spy

import com.rudderstack.sdk.kotlin.android.Analytics
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-`Analytics`-instance directory of registered [SpyPlugin]s, keyed by string tag.
 *
 * The registry's lifecycle is tied to the active SDK instance — [com.rudderstack.testapp.TestApp]
 * creates a fresh registry on every `initAnalytics` and clears it on `shutdownAnalytics`. Tag
 * collisions across re-inits are a non-issue: the previous registry is gone before the new one
 * exists.
 *
 * **Loud failures over silent recovery.** Both [add] and [remove] throw on a tag-state mismatch
 * (already-registered / never-registered). Scenario authoring shouldn't paper over a tag bug —
 * the dispatcher translates the throw into an `EVENT_TYPE_ERROR` broadcast and the interpreter
 * reports the Step as failed. Easier to debug than a silent no-op.
 *
 * Thread-safe for the add/remove pair via [ConcurrentHashMap]. [clear] is a teardown hook
 * called only from `shutdownAnalytics`, so concurrent reads are not a concern there.
 *
 * @param sink Shared sink for every plugin in this registry. One per SUT process is sufficient —
 *   `tag` lives on each emitted observation so the driver can route after the fact.
 */
class SpyPluginRegistry internal constructor(private val sink: SpySink) {

    private val plugins = ConcurrentHashMap<String, SpyPlugin>()

    /**
     * Register a new SpyPlugin under [tag] and add it to [analytics]'s plugin chain.
     * Throws if a plugin is already registered under the same tag.
     */
    fun add(tag: String, analytics: Analytics) {
        val plugin = SpyPlugin(tag, sink)
        val previous = plugins.putIfAbsent(tag, plugin)
        if (previous != null) {
            error("SpyPlugin with tag '$tag' is already registered")
        }
        analytics.add(plugin)
    }

    /**
     * Deregister the SpyPlugin previously added under [tag] from [analytics]'s plugin chain.
     * Throws if no plugin is registered under [tag].
     */
    fun remove(tag: String, analytics: Analytics) {
        val plugin = plugins.remove(tag)
            ?: error("No SpyPlugin registered under tag '$tag'")
        analytics.remove(plugin)
    }

    /**
     * Tear down every registered plugin, removing each from [analytics] (if non-null) before
     * forgetting the mapping. Called from `shutdownAnalytics` — by then [analytics] may already
     * be null because the caller is mid-shutdown.
     */
    fun clear(analytics: Analytics?) {
        plugins.values.forEach { plugin -> analytics?.remove(plugin) }
        plugins.clear()
    }
}
