package com.rudderstack.android.sdk

import android.app.Application
import com.rudderstack.android.sdk.storage.AndroidStorageProvider
import com.rudderstack.kotlin.sdk.Configuration
import com.rudderstack.kotlin.sdk.internals.logger.Logger
import com.rudderstack.kotlin.sdk.internals.policies.FlushPolicy

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
 * @param trackDeeplinks Flag to enable or disable automatic tracking of deeplink events. Defaults to `true`.
 * @param collectDeviceId Flag to enable or disable automatic collection of the device's ID. Defaults to `true`.
 * @param writeKey The write key used to authenticate and send data to the backend. This field is required.
 * @param dataPlaneUrl The URL of the data plane to which events are sent. This field is required.
 * @param controlPlaneUrl The URL of the control plane, used for remote configuration management. Defaults to `DEFAULT_CONTROL_PLANE_URL`.
 * @param logger The logger instance used for SDK logging. By default, it uses an Android logger with the `DEBUG` log level.
 * @param flushPolicies A list of flush policies defining when and how events should be sent to the backend. Defaults to `DEFAULT_FLUSH_POLICIES`.
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
    val collectDeviceId: Boolean = true,
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
