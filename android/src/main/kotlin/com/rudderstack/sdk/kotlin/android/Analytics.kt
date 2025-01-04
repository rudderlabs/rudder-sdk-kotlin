package com.rudderstack.sdk.kotlin.android

import android.app.Activity
import androidx.navigation.NavController
import androidx.navigation.NavController.OnDestinationChangedListener
import com.rudderstack.sdk.kotlin.android.logger.AndroidLogger
import com.rudderstack.sdk.kotlin.android.plugins.AndroidLifecyclePlugin
import com.rudderstack.sdk.kotlin.android.plugins.AppInfoPlugin
import com.rudderstack.sdk.kotlin.android.plugins.DeeplinkPlugin
import com.rudderstack.sdk.kotlin.android.plugins.DeviceInfoPlugin
import com.rudderstack.sdk.kotlin.android.plugins.LocaleInfoPlugin
import com.rudderstack.sdk.kotlin.android.plugins.NetworkInfoPlugin
import com.rudderstack.sdk.kotlin.android.plugins.OSInfoPlugin
import com.rudderstack.sdk.kotlin.android.plugins.ScreenInfoPlugin
import com.rudderstack.sdk.kotlin.android.plugins.TimezoneInfoPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.DestinationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.DeviceModeDestinationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleManagementPlugin
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ProcessLifecycleManagementPlugin
import com.rudderstack.sdk.kotlin.android.plugins.screenrecording.ActivityTrackingPlugin
import com.rudderstack.sdk.kotlin.android.plugins.screenrecording.NavControllerTrackingPlugin
import com.rudderstack.sdk.kotlin.android.plugins.sessiontracking.DEFAULT_SESSION_ID
import com.rudderstack.sdk.kotlin.android.plugins.sessiontracking.SessionTrackingPlugin
import com.rudderstack.sdk.kotlin.android.state.NavContext
import com.rudderstack.sdk.kotlin.android.storage.provideAndroidStorage
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.platform.Platform
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.FlowState
import com.rudderstack.sdk.kotlin.core.internals.utils.isAnalyticsActive
import com.rudderstack.sdk.kotlin.core.provideAnalyticsConfiguration
import org.jetbrains.annotations.ApiStatus.Experimental

private const val MIN_SESSION_ID_LENGTH = 10

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
    configuration,
    analyticsConfiguration = provideAnalyticsConfiguration(
        storage = provideAndroidStorage(configuration.writeKey, configuration.application)
    )
) {

    private var navControllerTrackingPlugin: NavControllerTrackingPlugin? = null

    private val navContextState by lazy {
        FlowState(NavContext.initialState())
    }

    internal val activityLifecycleManagementPlugin = ActivityLifecycleManagementPlugin()
    internal val processLifecycleManagementPlugin = ProcessLifecycleManagementPlugin()
    private var deviceModeDestinationPlugin: DeviceModeDestinationPlugin? = null
    private val sessionTrackingPlugin = SessionTrackingPlugin()

    init {
        setup()
    }

    /**
     * Starts a new session with the given optional session ID.
     */
    @JvmOverloads
    fun startSession(sessionId: Long? = null) {
        if (!isAnalyticsActive()) return

        if (sessionId != null && sessionId.toString().length < MIN_SESSION_ID_LENGTH) {
            LoggerAnalytics.error("Session Id should be at least $MIN_SESSION_ID_LENGTH digits.")
            return
        }
        val newSessionId = sessionId ?: sessionTrackingPlugin.generateSessionId()
        sessionTrackingPlugin.startSession(sessionId = newSessionId, isSessionManual = true)
    }

    /**
     * Ends the current session.
     */
    fun endSession() {
        if (!isAnalyticsActive()) return

        sessionTrackingPlugin.endSession()
    }

    /**
     * Returns the current session ID.
     *
     * @return The current session ID.
     */
    fun getSessionId(): Long? {
        if (!isAnalyticsActive() || sessionTrackingPlugin.sessionId == DEFAULT_SESSION_ID) return null

        return sessionTrackingPlugin.sessionId
    }

    /**
     * Resets the user identity, clearing the user ID, traits, and external IDs.
     * If clearAnonymousId is true, clears the existing anonymous ID and generate a new one.
     *
     * @param clearAnonymousId A boolean flag to determine whether to clear the anonymous ID. Defaults to false.
     */
    override fun reset(clearAnonymousId: Boolean) {
        if (!isAnalyticsActive()) return

        super.reset(clearAnonymousId)

        sessionTrackingPlugin.refreshSession()
        this.deviceModeDestinationPlugin?.reset()
    }

    override fun flush() {
        if (!isAnalyticsActive()) return

        super.flush()

        this.deviceModeDestinationPlugin?.flush()
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
        if (!isAnalyticsActive()) return

        if (navControllerTrackingPlugin == null) {
            navControllerTrackingPlugin = NavControllerTrackingPlugin(navContextState).also {
                add(it)
            }
        }

        navContextState.dispatch(
            action = NavContext.AddNavContextAction(
                navContext = NavContext(
                    navController = navController,
                    callingActivity = activity
                )
            )
        )
    }

    fun addDestination(plugin: DestinationPlugin) {
        if (!isAnalyticsActive()) return

        synchronized(this) {
            if (deviceModeDestinationPlugin == null) {
                deviceModeDestinationPlugin = DeviceModeDestinationPlugin().also { add(it) }
            }
        }

        deviceModeDestinationPlugin?.addDestination(plugin)
    }

    fun removeDestination(plugin: DestinationPlugin) {
        if (!isAnalyticsActive()) return

        deviceModeDestinationPlugin?.removeDestination(plugin)
    }

    private fun setup() {
        setLogger(logger = AndroidLogger())
        add(DeviceInfoPlugin())
        add(AppInfoPlugin())
        add(NetworkInfoPlugin())
        add(LocaleInfoPlugin())
        add(OSInfoPlugin())
        add(ScreenInfoPlugin())
        add(TimezoneInfoPlugin())
        add(sessionTrackingPlugin)

        // Add these plugins at last in chain
        add(AndroidLifecyclePlugin())
        add(DeeplinkPlugin())
        add(ActivityTrackingPlugin())

        // adding lifecycle management plugins last so that lifecycle callbacks are invoked after all the observers in plugins are added.
        add(processLifecycleManagementPlugin)
        add(activityLifecycleManagementPlugin)
    }

    override fun getPlatformType(): PlatformType = PlatformType.Mobile
}
