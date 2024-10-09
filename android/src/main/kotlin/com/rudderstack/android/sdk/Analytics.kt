package com.rudderstack.android.sdk

import androidx.navigation.NavController
import androidx.navigation.NavController.OnDestinationChangedListener
import com.rudderstack.android.sdk.plugins.AndroidLifecyclePlugin
import com.rudderstack.android.sdk.plugins.AppInfoPlugin
import com.rudderstack.android.sdk.plugins.DeeplinkPlugin
import com.rudderstack.android.sdk.plugins.DeviceInfoPlugin
import com.rudderstack.android.sdk.plugins.LocaleInfoPlugin
import com.rudderstack.android.sdk.plugins.NetworkInfoPlugin
import com.rudderstack.android.sdk.plugins.OSInfoPlugin
import com.rudderstack.android.sdk.plugins.ScreenInfoPlugin
import com.rudderstack.android.sdk.plugins.ScreenRecordingPlugin
import com.rudderstack.android.sdk.plugins.TimezoneInfoPlugin
import com.rudderstack.android.sdk.state.NavControllerState
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.platform.Platform
import com.rudderstack.kotlin.sdk.internals.platform.PlatformType
import com.rudderstack.kotlin.sdk.internals.statemanagement.SingleThreadStore
import com.rudderstack.kotlin.sdk.internals.statemanagement.Store
import org.jetbrains.annotations.ApiStatus.Experimental

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

    /**
     * Tracks the destination changes for the given [NavController] and automatically sends screen events for it.
     *
     * ## Description
     * The [NavController] class is used to navigate in an app. Whenever a destination change occurs, it can be tracked
     * using [OnDestinationChangedListener]. This API uses this listener to track destination changes and sends the screen
     * events.
     *
     * ## Example
     * example code for Compose navigation:
     * ```
     * @Composable
     * fun SunflowerApp() {
     *     val navController = rememberNavController()
     *     LaunchedEffect("first_launch") {
     *         analytics.addNavigationDestinationTracking(navController)
     *     }
     *     DisposableEffect(Unit) {
     *         onDispose {
     *             analytics.removeNavigationDestinationTracking(navController)
     *         }
     *     }
     *     SunFlowerNavHost(
     *         navController = navController
     *     )
     * }
     * ```
     * Example code for Fragment navigation:
     * ```
     * class MainActivity : AppCompatActivity() {
     *
     *    override fun onCreate(savedInstanceState: Bundle?) {
     *         super.onCreate(savedInstanceState)
     *
     *         val binding = ActivityMainBinding.inflate(layoutInflater)
     *         setContentView(binding.root)
     *
     *         // Get the navigation host fragment from this Activity
     *         val navHostFragment = supportFragmentManager
     *             .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
     *         // Instantiate the navController using the NavHostFragment
     *         navController = navHostFragment.navController
     *         analytics.addNavigationDestinationTracking(navController)
     *     }
     *
     *     override fun onDestroy() {
     *         super.onDestroy()
     *         analytics.removeNavigationDestinationTracking(navController)
     *     }
     * }
     * ```
     * In case multiple [NavController]s are used, call this method for each of them.
     * To stop tracking destination changes for a [NavController], call [removeNavigationDestinationTracking]
     *
     * @param navController [NavController] to be tracked
     */
    @Synchronized
    @Experimental
    fun addNavigationDestinationTracking(navController: NavController) {
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

    /**
     * Removes the `navController` from tracking automatic screen events for destination changes.
     *
     * ## Description
     * This should be called in `onDestroy` of the `Activity` which hosts `NavHostFragment` in case of `fragments` or in a `DisposableEffect`
     * of parent composable of `NavHost` in case of composables.
     *
     * @param navController [NavController] to be removed.
     *
     */
    @Synchronized
    @Experimental
    fun removeNavigationDestinationTracking(navController: NavController) {
        navControllerStore.dispatch(
            action = NavControllerState.RemoveNavControllerAction(
                navController = navController
            )
        )
    }

    private fun setup() {
        add(DeviceInfoPlugin())
        add(AppInfoPlugin())
        add(NetworkInfoPlugin())
        add(LocaleInfoPlugin())
        add(OSInfoPlugin())
        add(ScreenInfoPlugin())
        add(TimezoneInfoPlugin())

        // Add these plugins at last in chain
        add(AndroidLifecyclePlugin())
        add(DeeplinkPlugin())
    }

    override fun getPlatformType(): PlatformType = PlatformType.Mobile
}
