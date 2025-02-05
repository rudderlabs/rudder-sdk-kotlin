package com.rudderstack.sampleapp.analytics

import android.app.Application
import com.rudderstack.android.sampleapp.BuildConfig
import com.rudderstack.integration.kotlin.adjust.AdjustIntegration
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.SessionConfiguration
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sampleapp.analytics.customplugins.AndroidAdvertisingIdPlugin
import com.rudderstack.sampleapp.analytics.customplugins.AndroidAdvertisingIdPlugin.Companion.isAdvertisingLibraryAvailable
import com.rudderstack.sampleapp.analytics.customplugins.OptionPlugin
import com.rudderstack.sampleapp.analytics.customplugins.SampleIntegrationPlugin
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object RudderAnalyticsUtils {

    lateinit var analytics: Analytics

    fun initialize(application: Application) {
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
                logLevel = Logger.LogLevel.VERBOSE,
            )
        )
        if (isAdvertisingLibraryAvailable()) {
            analytics.add(AndroidAdvertisingIdPlugin())
        }
        analytics.add(OptionPlugin(
            option = RudderOption(
                customContext = buildJsonObject {
                    put("key", "value")
                },
                integrations = buildJsonObject {
                    put("CleverTap", true)
                }
            )
        ))

        analytics.add(AdjustIntegration())

        val sampleIntegrationPlugin = SampleIntegrationPlugin()
        sampleIntegrationPlugin.add(object : Plugin {
            override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess
            override lateinit var analytics: com.rudderstack.sdk.kotlin.core.Analytics

            override suspend fun intercept(event: Event): Event? {
                if (event is TrackEvent && event.event == "Track Event 1") {
                    LoggerAnalytics.debug("SampleAmplitudePlugin: dropping event")
                    return null
                }
                return event
            }
        })
        sampleIntegrationPlugin.onDestinationReady { _, destinationResult ->
            when (destinationResult) {
                is Result.Success ->
                    LoggerAnalytics.debug("SampleAmplitudePlugin: destination ready")

                is Result.Failure ->
                    LoggerAnalytics.debug("SampleAmplitudePlugin: destination failed to initialise: ${destinationResult.error.message}.")
            }
        }
        analytics.addIntegration(sampleIntegrationPlugin)
    }
}
