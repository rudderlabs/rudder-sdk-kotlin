package com.rudderstack.android.sdk

import androidx.navigation.NavController
import com.rudderstack.android.sdk.plugins.AndroidLifecyclePlugin
import com.rudderstack.android.sdk.plugins.DeeplinkPlugin
import com.rudderstack.android.sdk.plugins.NavControllerState
import com.rudderstack.android.sdk.plugins.ScreenRecordingPlugin
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.platform.Platform
import com.rudderstack.kotlin.sdk.internals.platform.PlatformType
import com.rudderstack.kotlin.sdk.internals.statemanagement.SingleThreadStore
import com.rudderstack.kotlin.sdk.internals.statemanagement.Store

/**
 * `Analytics` class in the `com.rudderstack.android` package.
 *
 * This class extends the `Analytics` class from the `com.rudderstack.core` package, providing
 * additional functionality for analytics tracking and configuration on Android platform.
 *
 * ## Description
 * The `Analytics` class is used to initialize analytics tracking in an Android application using
 * RudderStack. It provides methods and properties inherited from the core `Analytics` class, which
 * can be configured using the provided `Configuration` object.
 *
 * @constructor Creates an instance of the `Analytics` class.
 *
 * @param configuration An instance of the `Configuration` class that specifies the settings for
 * initializing the analytics.
 *
 * ## Example
 * ```kotlin
 * val configuration = Configuration.Builder(context, "your_write_key")
 *     .trackLifecycleEvents(true)
 *     .recordScreenViews(true)
 *     .build()
 * val analytics = Analytics(configuration)
 * ```
 *
 * @see com.rudderstack.kotlin.Analytics
 */
class Analytics(
    configuration: Configuration,
) : Platform, Analytics(
    configuration
) {

    private var screenRecordingPlugin: ScreenRecordingPlugin? = null

    private val navControllerStore: Store<NavControllerState, NavControllerState.NavControllerAction> by lazy {
        SingleThreadStore(
            initialState = NavControllerState.initialState(),
            reducer = NavControllerState.NavControllerReducer()
        )
    }

    init {
        setup()
    }

    @Synchronized
    fun trackNavigation(navController: NavController) {
        if (screenRecordingPlugin == null) {
            screenRecordingPlugin = ScreenRecordingPlugin(navControllerStore).also {
                add(it)
            }
        }

        navControllerStore.dispatch(
            action = NavControllerState.AddNavControllerAction(
                navController = navController
            )
        )
    }

    @Synchronized
    fun removeNavController(navController: NavController) {
        navControllerStore.dispatch(
            action = NavControllerState.RemoveNavControllerAction(
                navController = navController
            )
        )
    }

    private fun setup() {
        add(AndroidLifecyclePlugin())
        add(DeeplinkPlugin())
    }

    override fun getPlatformType(): PlatformType = PlatformType.Mobile
}
