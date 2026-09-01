package com.rudderstack.sampleapp.analytics

import android.app.Application
import android.util.Log
import com.rudderstack.android.sampleapp.BuildConfig
import com.rudderstack.sampleapp.analytics.customlogger.CustomTimberLogger
import com.rudderstack.sampleapp.analytics.customplugins.AndroidAdvertisingIdPlugin
import com.rudderstack.sampleapp.analytics.customplugins.AndroidAdvertisingIdPlugin.Companion.isAdvertisingLibraryAvailable
import com.rudderstack.sampleapp.analytics.customplugins.ConsentCategoryProvider
import com.rudderstack.sampleapp.analytics.customplugins.ConsentPlugin
import com.rudderstack.sampleapp.analytics.customplugins.SampleCustomIntegrationPlugin
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.SessionConfiguration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementConfiguration
import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementProvider
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.logger
import com.rudderstack.sdk.kotlin.core.internals.utils.Result

object RudderAnalyticsUtils {

    lateinit var analytics: Analytics

    private val androidAdvertisingIdPlugin = AndroidAdvertisingIdPlugin()
    private val sampleIntegrationPlugin = SampleCustomIntegrationPlugin()
    private val consentProvider = DemoConsentProvider()

    /** A human-readable summary of the demo CMP's current choices, for the sample UI. */
    val consentSummary: String
        get() = "Allowed: ${consentProvider.allowedConsentIds.joinToString(", ")}\n" +
            "Denied: ${consentProvider.deniedConsentIds.joinToString(", ")}"

    /**
     * Initializes the RudderStack Analytics SDK with the application context.
     *
     * @param application The Android Application instance
     */
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
                    updateSessionOnBackgroundEvents = false,
                ),
                gzipEnabled = true,
                logger = CustomTimberLogger(),
                logLevel = Logger.LogLevel.VERBOSE,
                consentManagement = ConsentManagementConfiguration(
                    enabled = true,
                    provider = ConsentManagementProvider.CUSTOM,
                    allowedConsentIds = consentProvider.allowedConsentIds,
                    deniedConsentIds = consentProvider.deniedConsentIds,
                ),
            )
        )
        analytics.add(sampleIntegrationPlugin())
        analytics.add(ConsentPlugin(provider = consentProvider))
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
                    logger.debug("SampleCustomIntegrationPlugin: dropping event")
                    return null
                }
                return event
            }
        })
        sampleIntegrationPlugin.onDestinationReady { _, destinationResult ->
            when (destinationResult) {
                is Result.Success ->
                    Log.d("Rudder-Analytics", "SampleCustomIntegrationPlugin: destination ready")

                is Result.Failure ->
                    Log.d("Rudder-Analytics", "SampleCustomIntegrationPlugin: destination failed to initialise: ${destinationResult.error.message}.")
            }
        }
        return sampleIntegrationPlugin
    }

    /**
     * Flips the demo CMP's consent choices, as if the user changed them in a consent dialog.
     */
    fun toggleConsent() {
        consentProvider.toggleConsent()
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

/**
 * Stands in for a real CMP in this sample. A production app would back these lists with its
 * Consent Management Platform's current state and call [onConsentChanged] from its callback.
 */
class DemoConsentProvider : ConsentCategoryProvider {

    override var allowedConsentIds: List<String> = listOf("marketing", "analytics")
        private set

    override var deniedConsentIds: List<String> = listOf("advertising")
        private set

    override var onConsentChanged: (() -> Unit)? = null

    /** Flips the demo consent set, as if the user changed their choices in the CMP. */
    fun toggleConsent() {
        val previouslyAllowed = allowedConsentIds
        allowedConsentIds = deniedConsentIds
        deniedConsentIds = previouslyAllowed
        onConsentChanged?.invoke()
    }
}
