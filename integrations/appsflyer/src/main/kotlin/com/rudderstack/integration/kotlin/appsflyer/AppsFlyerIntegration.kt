package com.rudderstack.integration.kotlin.appsflyer

import com.appsflyer.AppsFlyerLib
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.StandardIntegration
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.android.utils.getBoolean
import com.rudderstack.sdk.kotlin.android.utils.getString
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.serialization.json.JsonObject

internal const val APPSFLYER_KEY = "AppsFlyer"

internal const val CREATIVE = "creative"
private const val USE_RICH_EVENT_NAME = "useRichEventName"

/**
 * AppsFlyer Integration Plugin. See [IntegrationPlugin] for more info.
 */
@Suppress("TooManyFunctions")
class AppsFlyerIntegration : StandardIntegration, IntegrationPlugin() {

    private var appsFlyerInstance: AppsFlyerLib? = null
    private var isNewScreenEnabled: Boolean = false

    override val key: String
        get() = APPSFLYER_KEY

    public override fun create(destinationConfig: JsonObject) {
        // Initialize AppsFlyer instance - equivalent to Java getUnderlyingInstance()
        if (appsFlyerInstance == null) {
            appsFlyerInstance = provideAppsFlyerInstance()
        }

        // Extract configuration settings - equivalent to Java constructor logic
        extractConfiguration(destinationConfig)
    }

    internal fun provideAppsFlyerInstance(): AppsFlyerLib {
        return AppsFlyerLib.getInstance()
    }

    private fun extractConfiguration(destinationConfig: JsonObject) {
        // Extract useRichEventName setting - equivalent to Java constructor logic
        isNewScreenEnabled = destinationConfig.getBoolean(USE_RICH_EVENT_NAME) ?: false
    }

    override fun update(destinationConfig: JsonObject) {
        // Update configuration without re-initialization - Kotlin-specific method
        // Only update configuration settings, don't re-initialize AppsFlyer instance
        extractConfiguration(destinationConfig)
    }

    override fun getDestinationInstance(): Any? {
        return appsFlyerInstance
    }

    override fun identify(payload: IdentifyEvent) {
        // Set customer user ID - equivalent to Java setCustomerUserId
        payload.userId
            .takeIf { it.isNotEmpty() }
            ?.let { appsFlyerInstance?.setCustomerUserId(it) }

        // Set user email from traits - equivalent to Java setUserEmails
        analytics.traits
            ?.get("email")
            ?.getString()
            ?.takeIf { it.isNotEmpty() }
            ?.let { appsFlyerInstance?.setUserEmails(it) }
    }

    override fun track(payload: TrackEvent) {
        val (appsFlyerEventName, appsFlyerEventProps) = mapEventToAppsFlyer(payload.event, payload.properties)

        // Attach all custom properties (filtering reserved keywords)
        attachAllCustomProperties(appsFlyerEventProps, payload.properties)

        // Log event to AppsFlyer - equivalent to Java AppsFlyerLib.getInstance().logEvent()
        appsFlyerInstance?.logEvent(analytics.application, appsFlyerEventName, appsFlyerEventProps)
    }

    override fun screen(payload: ScreenEvent) {
        val screenName = if (isNewScreenEnabled) {
            // Rich event naming - equivalent to Java isNewScreenEnabled logic
            when {
                payload.screenName.isNotEmpty() -> "Viewed ${payload.screenName} Screen"
                payload.properties.getString("name")?.isNotEmpty() == true -> {
                    "Viewed ${payload.properties.getString("name")} Screen"
                }
                else -> "Viewed Screen"
            }
        } else {
            // Simple screen event name - equivalent to Java default behavior
            "screen"
        }

        // Log screen event to AppsFlyer - equivalent to Java logEvent call
        appsFlyerInstance?.logEvent(analytics.application, screenName, payload.properties.toMutableMap())
    }
}
