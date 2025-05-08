package com.rudderstack.sampleapp.analytics

import android.app.Application
import com.rudderstack.android.sampleapp.BuildConfig
import com.rudderstack.sampleapp.analytics.customlogger.CustomTimberLogger
import com.rudderstack.sampleapp.analytics.customplugins.AndroidAdvertisingIdPlugin
import com.rudderstack.sampleapp.analytics.customplugins.AndroidAdvertisingIdPlugin.Companion.isAdvertisingLibraryAvailable
import com.rudderstack.sampleapp.analytics.customplugins.SampleCustomIntegrationPlugin
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.SessionConfiguration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.utils.Result

object RudderAnalyticsUtils {

    lateinit var analytics: Analytics

    private val androidAdvertisingIdPlugin = AndroidAdvertisingIdPlugin()
    private val sampleIntegrationPlugin = SampleCustomIntegrationPlugin()

    /**
     * Initializes the RudderStack Analytics SDK with the application context.
     *
     * @param application The Android Application instance
     */
    fun initialize(application: Application) {
        // setting the LogLevel
        LoggerAnalytics.logLevel = Logger.LogLevel.VERBOSE
        // setting a custom logger
        LoggerAnalytics.setLogger(CustomTimberLogger())

        analytics = Analytics(
            configuration = Configuration(
                trackApplicationLifecycleEvents = true,
                writeKey = BuildConfig.WRITE_KEY,
                application = application,
                dataPlaneUrl = BuildConfig.DATA_PLANE_URL,
                sessionConfiguration = SessionConfiguration(
                    automaticSessionTracking = true,
                    sessionTimeoutInMillis = 3000,
                ),
                gzipEnabled = true,
            )
        )
//        analytics.add(sampleIntegrationPlugin())
//        analytics.add(BrazeIntegration()) // requires a minSDK version of 25
//        analytics.add(AdjustIntegration())
    }

    /**
     * Plugin instance for sample integration demonstrations
     */

    private fun sampleIntegrationPlugin(): IntegrationPlugin {
        sampleIntegrationPlugin.add(object : Plugin {
            override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess
            override lateinit var analytics: com.rudderstack.sdk.kotlin.core.Analytics

            override suspend fun intercept(event: Event): Event? {
                if (event is TrackEvent && event.event == "Track Event 1") {
                    LoggerAnalytics.debug("SampleCustomIntegrationPlugin: dropping event")
                    return null
                }
                return event
            }
        })
        sampleIntegrationPlugin.onDestinationReady { _, destinationResult ->
            when (destinationResult) {
                is Result.Success ->
                    LoggerAnalytics.debug("SampleCustomIntegrationPlugin: destination ready")

                is Result.Failure ->
                    LoggerAnalytics.debug("SampleCustomIntegrationPlugin: destination failed to initialise: ${destinationResult.error.message}.")
            }
        }
        return sampleIntegrationPlugin
    }

    /**
     * Adds the Android Advertising ID plugin to the analytics instance.
     * This enables tracking and handling of advertising IDs in the analytics flow.
     */

    fun addAndroidAdvertisingIdPlugin() {
        if (isAdvertisingLibraryAvailable()) {
            analytics.add(androidAdvertisingIdPlugin)
        }
    }

    /**
     * Removes the Android Advertising ID plugin from the analytics instance.
     * This disables tracking and handling of advertising IDs in the analytics flow.
     */

    fun removeAndroidAdvertisingIdPlugin() {
        analytics.remove(androidAdvertisingIdPlugin)
    }
}
