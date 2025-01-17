package com.rudderstack.sdk.kotlin.android.utils

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics

/**
 * **WARNING: Don't add any utils function in this file that don't need to run on main thread.**
 * For utils function that don't require main thread, use `AnalyticsUtils.kt` file.
 */

private val MAIN_DISPATCHER = Dispatchers.Main.immediate

/**
 * Runs a block on the main thread.
 *
 * @param block The block which needs to be executed.
 */
@DelicateCoroutinesApi
internal fun AndroidAnalytics.runOnMainThread(block: suspend () -> Unit) = analyticsScope.launch(MAIN_DISPATCHER) {
    block()
}
