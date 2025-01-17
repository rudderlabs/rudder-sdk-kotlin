package com.rudderstack.sdk.kotlin.android.utils

import android.app.Application
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.core.Analytics
import kotlinx.coroutines.launch

/**
 * Runs a suspend block on a coroutine launched in `analyticsScope` with `analyticsDispatcher`
 *
 * @param block The suspend block which needs to be executed.
 */
internal fun Analytics.runOnAnalyticsThread(block: suspend () -> Unit) = analyticsScope.launch(analyticsDispatcher) {
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
