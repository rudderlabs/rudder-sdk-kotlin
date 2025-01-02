package com.rudderstack.sdk.kotlin.android

import android.app.Application
import com.rudderstack.sdk.kotlin.core.Configuration
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.policies.FlushPolicy

internal const val DEFAULT_SESSION_TIMEOUT_IN_MILLIS = 300_000L

/**
 * `Configuration` data class used for initializing RudderStack analytics in an Android application.
 *
 * This class defines the necessary configuration settings required to set up analytics tracking
 * using RudderStack. It provides properties for specifying the application context, write key,
 * data plane URL, and a logger for tracking and debugging purposes.
 *
 * This class is annotated with `@JvmOverloads` to provide multiple overloads for the constructor
 * when used from Java.
 *
 * ## Properties
 * - `application`: The Android `Application` instance used to initialize the analytics library.
 * - `writeKey`: The write key obtained from the RudderStack dashboard, which is used to authenticate
 *   requests.
 * - `dataPlaneUrl`: The URL of the RudderStack data plane where events are sent.
 * - `logger`: An instance of `Logger` used for logging and debugging. The default value is
 *   `AndroidLogger` with an initial log level set to `Logger.LogLevel.DEBUG`.
 *
 * ## Constructor
 * @param application The application context. Required for accessing Android-specific functionality and tracking lifecycle events.
 * @param trackApplicationLifecycleEvents Flag to enable or disable automatic tracking of application lifecycle events.
 * Defaults to `true`, enabling the SDK to track app start, background, and foreground events.
 * @param trackDeepLinks Flag to enable or disable automatic tracking of deeplink events. Defaults to `true`.
 * @param collectDeviceId Flag to enable or disable automatic collection of the device's ID. Defaults to `true`.
 * @param sessionConfiguration Configuration settings for session tracking. Defaults to `SessionConfiguration()`.
 * @param trackActivities automatically tracks activities, calling screen events for them, defaults to `false`.
 * @param writeKey The write key used to authenticate and send data to the backend. This field is required.
 * @param dataPlaneUrl The URL of the data plane to which events are sent. This field is required.
 * @param controlPlaneUrl The URL of the control plane, used for remote configuration management. Defaults to `DEFAULT_CONTROL_PLANE_URL`.
 * @param logLevel The log level used for SDK logging. By default, it uses the `DEBUG` log level.
 * @param flushPolicies A list of flush policies defining when and how events should be sent to the backend. Defaults to `DEFAULT_FLUSH_POLICIES`.
 * @param gzipEnabled Flag to enable or disable GZIP compression for network requests. Defaults to `DEFAULT_GZIP_STATUS`.
 *
 * ## Example
 * ```kotlin
 * val configuration = Configuration(
 *     application = appInstance,
 *     writeKey = "your_write_key",
 *     dataPlaneUrl = "https://your-dataplane-url",
 *     logger = AndroidLogger(Logger.LogLevel.INFO) // Optional: Customize the logger if needed
 * )
 * ```
 *
 * This `Configuration` instance can then be used to initialize the `Analytics` object for RudderStack.
 *
 * @see com.rudderstack.sdk.kotlin.core.Configuration
 */
data class Configuration @JvmOverloads constructor(
    val application: Application,
    val trackApplicationLifecycleEvents: Boolean = true,
    val trackDeepLinks: Boolean = true,
    val trackActivities: Boolean = false,
    val collectDeviceId: Boolean = true,
    val sessionConfiguration: SessionConfiguration = SessionConfiguration(),
    override val writeKey: String,
    override val dataPlaneUrl: String,
    override val controlPlaneUrl: String = DEFAULT_CONTROL_PLANE_URL,
    override val logLevel: Logger.LogLevel = Logger.DEFAULT_LOG_LEVEL,
    override val flushPolicies: List<FlushPolicy> = DEFAULT_FLUSH_POLICIES,
    override val gzipEnabled: Boolean = DEFAULT_GZIP_STATUS
) : Configuration(
    writeKey = writeKey,
    dataPlaneUrl = dataPlaneUrl,
    logLevel = logLevel,
    flushPolicies = flushPolicies,
)

/**
 * Data class for configuring session tracking in analytics.
 *
 * This class defines the necessary configuration settings required to set up session tracking.
 * It provides properties for enabling automatic session tracking, setting the session
 * timeout duration, and specifying the session ID.
 *
 * @param automaticSessionTracking Flag to enable or disable automatic session tracking. Defaults to `true`.
 * @param sessionTimeoutInMillis The duration in milliseconds after which a session is considered timed out. Defaults to `300_000` milliseconds (5 minutes).
 *
 * This `SessionConfiguration` instance can then be used to configure session tracking in the [Configuration].
 *
 */
data class SessionConfiguration(
    val automaticSessionTracking: Boolean = true,
    val sessionTimeoutInMillis: Long = DEFAULT_SESSION_TIMEOUT_IN_MILLIS,
)
