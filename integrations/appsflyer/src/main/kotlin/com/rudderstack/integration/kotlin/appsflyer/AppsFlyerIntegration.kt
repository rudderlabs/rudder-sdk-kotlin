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

private const val USE_RICH_EVENT_NAME = "useRichEventName"
private const val EMAIL = "email"
private const val NAME = "name"
private const val VIEWED = "Viewed"
private const val SCREEN = "Screen"
private const val SCREEN_EVENT = "screen"

/**
 * AppsFlyer Integration Plugin. See [IntegrationPlugin] for more info.
 */
class AppsFlyerIntegration : StandardIntegration, IntegrationPlugin() {

    private var appsFlyerInstance: AppsFlyerLib? = null
    private var isNewScreenEnabled: Boolean = false

    override val key: String
        get() = APPSFLYER_KEY

    public override fun create(destinationConfig: JsonObject) {
        if (appsFlyerInstance == null) {
            appsFlyerInstance = provideAppsFlyerInstance()
        }

        extractConfiguration(destinationConfig)
    }

    internal fun provideAppsFlyerInstance(): AppsFlyerLib? {
        return AppsFlyerLib.getInstance()
    }

    private fun extractConfiguration(destinationConfig: JsonObject) {
        isNewScreenEnabled = destinationConfig.getBoolean(USE_RICH_EVENT_NAME) ?: false
    }

    override fun update(destinationConfig: JsonObject) {
        extractConfiguration(destinationConfig)
    }

    override fun getDestinationInstance(): Any? {
        return appsFlyerInstance
    }

    override fun identify(payload: IdentifyEvent) {
        payload.userId
            .takeIf { it.isNotEmpty() }
            ?.let { appsFlyerInstance?.setCustomerUserId(it) }

        payload.traits
            ?.get(EMAIL)
            ?.getString()
            ?.takeIf { it.isNotEmpty() }
            ?.let { appsFlyerInstance?.setUserEmails(it) }
    }

    override fun track(payload: TrackEvent) {
        val (appsFlyerEventName, appsFlyerEventProps) = mapEventToAppsFlyer(
            eventName = payload.event,
            properties = payload.properties
        )

        appsFlyerInstance?.logEvent(analytics.application, appsFlyerEventName, appsFlyerEventProps)
    }

    override fun screen(payload: ScreenEvent) {
        val screenName = if (isNewScreenEnabled) {
            when {
                payload.screenName.isNotEmpty() -> "$VIEWED ${payload.screenName} $SCREEN"
                payload.properties.getString(NAME)?.isNotEmpty() == true -> {
                    "$VIEWED ${payload.properties.getString(NAME)} $SCREEN"
                }
                else -> "$VIEWED $SCREEN"
            }
        } else {
            SCREEN_EVENT
        }

        appsFlyerInstance?.logEvent(analytics.application, screenName, payload.properties.toMutableMap())
    }
}
