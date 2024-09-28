package com.rudderstack.android.sdk

import android.app.Application
import com.rudderstack.android.sdk.storage.AndroidStorageProvider
import com.rudderstack.kotlin.Configuration
import com.rudderstack.kotlin.internals.logger.Logger
import com.rudderstack.kotlin.internals.policies.FlushPolicy

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
 * @param application The Android `Application` instance required for initializing the analytics library.
 * @param trackApplicationLifecycleEvents automatically send track for Lifecycle events (eg: Application Opened, Application Backgrounded, etc.), defaults to `true`
 * @param trackDeeplinks automatically tracks deeplinks opened based on the intent fired, defaults to `true`.
 * @param writeKey The write key for authenticating with RudderStack.
 * @param dataPlaneUrl The URL of the RudderStack data plane to which events are sent.
 * @param logger An instance of `Logger` for debugging (optional; default is `AndroidLogger` with log level set to DEBUG).
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
 * @see com.rudderstack.kotlin.Configuration
 */
data class Configuration @JvmOverloads constructor(
    val application: Application,
    val trackApplicationLifecycleEvents: Boolean = true,
    val trackDeeplinks: Boolean = true,
    override val writeKey: String,
    override val dataPlaneUrl: String,
    override val controlPlaneUrl: String = DEFAULT_CONTROL_PLANE_URL,
    override val logger: Logger = AndroidLogger(initialLogLevel = Logger.LogLevel.DEBUG),
    override var flushPolicies: List<FlushPolicy> = DEFAULT_FLUSH_POLICIES,
) : Configuration(
    writeKey = writeKey,
    dataPlaneUrl = dataPlaneUrl,
    storage = AndroidStorageProvider.getStorage(writeKey, application),
    logger = logger,
    flushPolicies = flushPolicies,
)
