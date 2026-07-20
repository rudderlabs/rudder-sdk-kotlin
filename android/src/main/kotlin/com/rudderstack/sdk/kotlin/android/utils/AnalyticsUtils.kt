package com.rudderstack.sdk.kotlin.android.utils

import com.rudderstack.sdk.kotlin.core.Analytics
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics

private val MAIN_DISPATCHER = Dispatchers.Main.immediate

/**
 * Runs a suspend block on a coroutine launched in `analyticsScope` with `analyticsDispatcher`
 *
 * @param block The suspend block which needs to be executed.
 */
internal fun Analytics.runOnAnalyticsThread(block: suspend () -> Unit) = analyticsScope.launch(analyticsDispatcher) {
    block()
}

/**
 * Runs a suspend block on a coroutine launched in `analyticsScope` with `analyticsDispatcher`, once
 * [previousJob] has completed.
 *
 * Passing the returned [Job] as the [previousJob] of the next call chains the blocks, so that they run in
 * submission order. Launching them independently would not guarantee this, as `analyticsDispatcher` is
 * multi-threaded. Callers must submit from a single thread, so that the chain is built race free.
 *
 * @param previousJob The job to wait for before running [block], or `null` to run it right away.
 * @param block The suspend block which needs to be executed.
 * @return The [Job] of the launched coroutine, to be passed as the [previousJob] of the next call.
 */
internal fun Analytics.runOnAnalyticsThreadAfter(previousJob: Job?, block: suspend () -> Unit): Job = runOnAnalyticsThread {
    previousJob?.join()
    block()
}

/**
 * Runs a block on the main thread.
 *
 * @param block The block which needs to be executed.
 */
@DelicateCoroutinesApi
internal fun AndroidAnalytics.runOnMainThread(block: suspend () -> Unit) = analyticsScope.launch(MAIN_DISPATCHER) {
    block()
}
