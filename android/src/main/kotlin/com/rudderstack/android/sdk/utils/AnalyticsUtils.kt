package com.rudderstack.android.sdk.utils

import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.logger.LoggerAnalytics
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.rudderstack.android.sdk.Analytics as AndroidAnalytics

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
 * Runs a block on the main thread.
 *
 * @param block The block which needs to be executed.
 */
@DelicateCoroutinesApi
internal fun AndroidAnalytics.runOnMainThread(block: () -> Unit) = analyticsScope.launch(MAIN_DISPATCHER) {
    block()
}

/**
 * Logs an error `message` and then throws a `throwable` (`IllegalStateException` by default).
 *
 * @param message The error message to be logged.
 * @param throwable The error to be thrown.
 */
internal fun Analytics.logAndThrowError(message: String, throwable: Throwable? = null): Nothing {
    LoggerAnalytics.error(message)
    throw throwable ?: error(message)
}
