package com.rudderstack.sdk.kotlin.android.utils

import android.app.Application
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.core.Analytics
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
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
 * Runs a block on the main thread.
 *
 * @param block The block which needs to be executed.
 */
@DelicateCoroutinesApi
internal fun AndroidAnalytics.runOnMainThread(block: suspend () -> Unit) = analyticsScope.launch(MAIN_DISPATCHER) {
    block()
}

/**
 * Provides access to the [Application] instance associated with the Android Analytics object.
 *
 * This property retrieves the application instance from the [Configuration] of the Android Analytics object,
 * ensuring that the correct application context is available for performing various SDK operations.
 *
 * @return The [Application] instance tied to the current [Analytics] configuration.
 */
internal val Analytics.application: Application
    get() = (this.configuration as Configuration).application
