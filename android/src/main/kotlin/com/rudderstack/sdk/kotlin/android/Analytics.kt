package com.rudderstack.sdk.kotlin.android

import android.app.Activity
import androidx.navigation.NavController
import androidx.navigation.NavController.OnDestinationChangedListener
import com.rudderstack.sdk.kotlin.android.connectivity.AndroidConnectivityObserverPlugin
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
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationsManagementPlugin
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleManagementPlugin
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ProcessLifecycleManagementPlugin
import com.rudderstack.sdk.kotlin.android.plugins.screenrecording.ActivityTrackingPlugin
import com.rudderstack.sdk.kotlin.android.plugins.screenrecording.NavContext
import com.rudderstack.sdk.kotlin.android.plugins.screenrecording.NavControllerTrackingPlugin
import com.rudderstack.sdk.kotlin.android.plugins.sessiontracking.DEFAULT_SESSION_ID
import com.rudderstack.sdk.kotlin.android.plugins.sessiontracking.SessionTrackingPlugin
import com.rudderstack.sdk.kotlin.android.storage.provideAndroidStorage
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.platform.Platform
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.utils.isAnalyticsActive
import com.rudderstack.sdk.kotlin.core.internals.utils.isSourceEnabled
import com.rudderstack.sdk.kotlin.core.provideAnalyticsConfiguration

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
 * @see com.rudderstack.sdk.kotlin.core.Analytics
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
    internal val activityLifecycleManagementPlugin = ActivityLifecycleManagementPlugin()
    internal val processLifecycleManagementPlugin = ProcessLifecycleManagementPlugin()
    private val integrationsManagementPlugin = IntegrationsManagementPlugin()
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
        val newSessionId = sessionId ?: sessionTrackingPlugin.sessionManager.generateSessionId()
        sessionTrackingPlugin.sessionManager.startSession(sessionId = newSessionId, isSessionManual = true)
    }

    /**
     * Ends the current session.
     */
    fun endSession() {
        if (!isAnalyticsActive()) return

        sessionTrackingPlugin.sessionManager.endSession()
    }

    /**
     * Resets the user identity, clears the existing anonymous ID and
     * generate a new one, also clears the user ID and traits.
     */
    override fun reset() {
        if (!isAnalyticsActive()) return
        super.reset()

        sessionTrackingPlugin.sessionManager.refreshSession()

        if (!isSourceEnabled()) return

        integrationsManagementPlugin.reset()
    }

    override fun flush() {
        if (!isAnalyticsActive()) return

        super.flush()

        integrationsManagementPlugin.flush()
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
     * @param activity [Activity] of the `NavHostFragment` or the parent composable in which [navController] is instantiated.
     */
    @Synchronized
    fun setNavigationDestinationsTracking(navController: NavController, activity: Activity) {
        if (!isAnalyticsActive()) return

        if (navControllerTrackingPlugin == null) {
            navControllerTrackingPlugin = NavControllerTrackingPlugin().also {
                add(it)
            }
        }

        navControllerTrackingPlugin?.addContextAndObserver(
            navContext = NavContext(
                navController = navController,
                callingActivity = activity
            )
        )
    }

    /**
     * Adds a plugin to the plugin chain. Plugins can modify, enrich, or process events before they are sent to the server.
     *
     * **Note**: This API is also used to add an [IntegrationPlugin] which represents device mode integrations.
     *
     * @param plugin The plugin to be added to the plugin chain.
     */
    override fun add(plugin: Plugin) {
        if (!isAnalyticsActive()) return

        if (plugin is IntegrationPlugin) {
            integrationsManagementPlugin.addIntegration(plugin)
        } else {
            super.add(plugin)
        }
    }

    /**
     * Removes a plugin from the plugin chain.
     *
     * **Note**: This API is also used to remove an [IntegrationPlugin] which represents device mode integrations.
     *
     * @param plugin The plugin to be removed from the plugin chain.
     */
    override fun remove(plugin: Plugin) {
        if (!isAnalyticsActive()) return

        if (plugin is IntegrationPlugin) {
            integrationsManagementPlugin.removeIntegration(plugin)
        } else {
            super.remove(plugin)
        }
    }

    private fun setup() {
        setLogger(logger = AndroidLogger())
        add(AndroidConnectivityObserverPlugin(connectivityState))
        add(DeviceInfoPlugin())
        add(AppInfoPlugin())
        add(NetworkInfoPlugin())
        add(LocaleInfoPlugin())
        add(OSInfoPlugin())
        add(ScreenInfoPlugin())
        add(TimezoneInfoPlugin())
        add(sessionTrackingPlugin)
        add(integrationsManagementPlugin)

        // Add these plugins at last in chain
        add(AndroidLifecyclePlugin())
        add(DeeplinkPlugin())
        add(ActivityTrackingPlugin())

        // adding lifecycle management plugins last so that lifecycle callbacks are invoked after all the observers in plugins are added.
        add(processLifecycleManagementPlugin)
        add(activityLifecycleManagementPlugin)

        // Setup source config
        setupSourceConfig()
    }

    override fun getPlatformType(): PlatformType = PlatformType.Mobile

    /**
     * Returns the current session ID.
     */
    val sessionId: Long?
        get() {
            if (!isAnalyticsActive() || sessionTrackingPlugin.sessionManager.sessionId == DEFAULT_SESSION_ID) return null
            return sessionTrackingPlugin.sessionManager.sessionId
        }
}
