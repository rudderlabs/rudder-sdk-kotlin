package com.rudderstack.android.sdk

import android.app.Activity
import androidx.navigation.NavController
import androidx.navigation.NavController.OnDestinationChangedListener
import com.rudderstack.android.sdk.plugins.ActivityTrackingPlugin
import com.rudderstack.android.sdk.plugins.AndroidLifecyclePlugin
import com.rudderstack.android.sdk.plugins.AppInfoPlugin
import com.rudderstack.android.sdk.plugins.DeeplinkPlugin
import com.rudderstack.android.sdk.plugins.DeviceInfoPlugin
import com.rudderstack.android.sdk.plugins.LocaleInfoPlugin
import com.rudderstack.android.sdk.plugins.NetworkInfoPlugin
import com.rudderstack.android.sdk.plugins.OSInfoPlugin
import com.rudderstack.android.sdk.plugins.ScreenInfoPlugin
import com.rudderstack.android.sdk.plugins.TimezoneInfoPlugin
import com.rudderstack.android.sdk.plugins.screenrecording.NavControllerTrackingPlugin
import com.rudderstack.android.sdk.state.NavContext
import com.rudderstack.android.sdk.state.NavContextState
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

    private var navControllerTrackingPlugin: NavControllerTrackingPlugin? = null

    private val navContextStore: Store<NavContextState, NavContextState.NavContextAction> by lazy {
        SingleThreadStore(
            initialState = NavContextState.initialState(),
            reducer = NavContextState.NavContextReducer()
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
     * using [OnDestinationChangedListener]. This API uses [OnDestinationChangedListener] and activity's onStart callback to track destination
     * changes and sends automatic screen events for them.
     *
     * Note: This API will send screen events for the currentDestination when the app is foregrounded or when configuration
     * change occurs.
     *
     * ## Example
     * example code for Compose navigation:
     * ```
     * @Composable
     * fun SunflowerApp() {
     *     val navController = rememberNavController()
     *     LaunchedEffect("first_launch") {
     *         analytics.setNavigationDestinationsTracking(navController, this@MainActivity)
     *     }
     *
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
     *         analytics.setNavigationDestinationsTracking(navController, this@MainActivity)
     *     }
     * }
     * ```
     * In case multiple [NavController]s are used, call this method for each of them.
     *
     * @param navController [NavController] to be tracked
     * @param activity [Activity] of the [NavHostFragment] or the parent composable in which [navController] is instantiated.
     */
    @Synchronized
    @Experimental
    fun setNavigationDestinationsTracking(navController: NavController, activity: Activity) {
        if (navControllerTrackingPlugin == null) {
            navControllerTrackingPlugin = NavControllerTrackingPlugin(navContextStore).also {
                add(it)
            }
        }

        navContextStore.dispatch(
            action = NavContextState.AddNavContextAction(
                navContext = NavContext(
                    navController = navController,
                    callingActivity = activity
                )
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
        add(ActivityTrackingPlugin())
    }

    override fun getPlatformType(): PlatformType = PlatformType.Mobile
}
