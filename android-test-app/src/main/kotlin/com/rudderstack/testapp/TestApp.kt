package com.rudderstack.testapp

import android.app.Application
import com.rudderstack.scenarioengine.domain.step.Step
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.testapp.analytics.AnalyticsFactory
import com.rudderstack.testapp.ipc.Dispatcher
import com.rudderstack.testapp.spy.BroadcastSpySink
import com.rudderstack.testapp.spy.SpyPluginRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * The SUT app's [Application]. Owns the live [Analytics] instance, the per-process
 * [CoroutineScope] the IPC layer dispatches commands on, and the [Dispatcher] that
 * routes parsed commands to SDK calls.
 *
 * One [Analytics] instance at a time. Calling [initAnalytics] when one already exists
 * tears the previous one down first — the engine relies on this to repeatedly re-init
 * within a single test run.
 */
class TestApp : Application() {

    /** Long-lived scope used by the [com.rudderstack.testapp.ipc.CommandReceiver] to run dispatch off the main thread. */
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Single dispatcher instance; stateless aside from its reference to this app. */
    val dispatcher: Dispatcher by lazy { Dispatcher(this) }

    /** The currently-active SDK instance, or null before the first INIT / after [shutdownAnalytics]. */
    @Volatile
    var analytics: Analytics? = null
        private set

    /**
     * The registry of test-only SpyPlugins for the currently-active [analytics]. Lifetime is
     * tied to the SDK instance — a fresh registry is built in [initAnalytics] and cleared in
     * [shutdownAnalytics]. Null before the first INIT / after shutdown, mirroring [analytics].
     */
    @Volatile
    var spyPluginRegistry: SpyPluginRegistry? = null
        private set

    /**
     * Build a fresh [Analytics] from [step] and adopt it as the active instance.
     * Any prior instance is shut down first.
     */
    fun initAnalytics(step: Step.Init) {
        shutdownAnalytics()
        analytics = AnalyticsFactory.create(this, step)
        spyPluginRegistry = SpyPluginRegistry(BroadcastSpySink(applicationContext))
    }

    /** Tear down the active [Analytics] and its [SpyPluginRegistry], if any. Idempotent. */
    fun shutdownAnalytics() {
        spyPluginRegistry?.clear(analytics)
        spyPluginRegistry = null
        analytics?.shutdown()
        analytics = null
    }
}
