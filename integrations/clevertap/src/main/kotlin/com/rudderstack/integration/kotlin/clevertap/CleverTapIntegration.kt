@file:Suppress("TooManyFunctions")

package com.rudderstack.integration.kotlin.clevertap

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.os.Bundle
import com.clevertap.android.sdk.CleverTapAPI
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.SdkNotInitializedException
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.StandardIntegration
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.addLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.android.utils.removeLifecycleObserver
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import kotlinx.serialization.json.JsonObject
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics

private const val CLEVERTAP_KEY = "CleverTap"

/**
 * CleverTapIntegration is a plugin that sends events to the CleverTap Android SDK.
 */
@OptIn(InternalRudderApi::class)
class CleverTapIntegration : StandardIntegration, IntegrationPlugin(), ActivityLifecycleObserver {

    override val key: String
        get() = CLEVERTAP_KEY

    private var cleverTap: CleverTapAPI? = null

    public override fun create(destinationConfig: JsonObject) {
        if (cleverTap != null) return

        val config = destinationConfig.parseConfig<CleverTapDestinationConfig>(analytics.logger)
            ?: throw SdkNotInitializedException("CleverTapIntegration: Destination config is empty.")

        if (!config.hasValidCredentials()) {
            throw SdkNotInitializedException("CleverTapIntegration: Account ID or token is blank.")
        }

        cleverTap = initCleverTap(
            application = analytics.application,
            config = config,
            logLevel = analytics.configuration.logLevel,
        ) ?: throw SdkNotInitializedException("CleverTapIntegration: CleverTap SDK returned no instance.")

        (analytics as? AndroidAnalytics)?.addLifecycleObserver(this)
        analytics.logger.info("CleverTapIntegration: CleverTap SDK initialized.")
    }

    override fun getDestinationInstance(): Any? = cleverTap

    override fun identify(payload: IdentifyEvent) {
        runCatching {
            cleverTap?.onUserLogin(payload.toCleverTapProfile(analytics.logger))
        }.logOnFailure("CleverTapIntegration: Failed to send identify event.")
    }

    override fun track(payload: TrackEvent) {
        if (payload.event.isBlank()) return

        runCatching {
            when (val event = payload.properties.toCleverTapTrackEvent(payload.event, analytics.logger)) {
                is CleverTapTrackEvent.ChargedEvent -> cleverTap?.pushChargedEvent(event.chargeDetails, event.items)
                is CleverTapTrackEvent.CustomEvent -> pushCustomEvent(event)
            }
        }.logOnFailure("CleverTapIntegration: Failed to send track event '${payload.event}'.")
    }

    override fun screen(payload: ScreenEvent) {
        runCatching {
            pushCustomEvent(payload.properties.toCleverTapScreenEvent(payload.screenName))
        }.logOnFailure("CleverTapIntegration: Failed to send screen event '${payload.screenName}'.")
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        if (cleverTap == null) return

        setAppForeground(true)
        pushNotificationClickedEvent(activity.intent?.extras)
        pushDeepLink(activity.intent?.data)
    }

    override fun onActivityResumed(activity: Activity) {
        if (cleverTap == null) return

        runCatching {
            CleverTapAPI.onActivityResumed(activity)
        }.logOnFailure("CleverTapIntegration: Failed to handle activity resumed callback.")
    }

    override fun onActivityPaused(activity: Activity) {
        if (cleverTap == null) return

        runCatching {
            CleverTapAPI.onActivityPaused()
        }.logOnFailure("CleverTapIntegration: Failed to handle activity paused callback.")
    }

    override fun teardown() {
        super.teardown()
        (analytics as? AndroidAnalytics)?.removeLifecycleObserver(this)
        cleverTap = null
    }

    /**
     * Sends a CleverTap notification-clicked event for push payload extras.
     */
    fun pushNotificationClickedEvent(extras: Bundle?) {
        runCatching {
            cleverTap?.pushNotificationClickedEvent(extras)
        }.logOnFailure("CleverTapIntegration: Failed to send push notification clicked event.")
    }

    /**
     * Sends a deep link to CleverTap for attribution handling.
     */
    fun pushDeepLink(uri: Uri?) {
        runCatching {
            cleverTap?.pushDeepLink(uri)
        }.logOnFailure("CleverTapIntegration: Failed to send push deep link.")
    }

    /**
     * Sets CleverTap's app foreground state.
     */
    fun setAppForeground(isForeground: Boolean) {
        runCatching {
            CleverTapAPI.setAppForeground(isForeground)
        }.logOnFailure("CleverTapIntegration: Failed to set app foreground state.")
    }

    private fun pushCustomEvent(event: CleverTapTrackEvent.CustomEvent) {
        if (event.properties.isEmpty()) {
            cleverTap?.pushEvent(event.eventName)
        } else {
            cleverTap?.pushEvent(event.eventName, event.properties)
        }
    }

    private fun Result<*>.logOnFailure(message: String) {
        onFailure { analytics.logger.error(message, it) }
    }
}

private fun initCleverTap(
    application: Application,
    config: CleverTapDestinationConfig,
    logLevel: Logger.LogLevel,
): CleverTapAPI? {
    if (config.hasRegion()) {
        CleverTapAPI.changeCredentials(config.accountId, config.accountToken, config.region)
    } else {
        CleverTapAPI.changeCredentials(config.accountId, config.accountToken)
    }
    CleverTapAPI.setDebugLevel(logLevel.toCleverTapLogLevel())
    return CleverTapAPI.getDefaultInstance(application)
}

internal fun Logger.LogLevel.toCleverTapLogLevel(): CleverTapAPI.LogLevel = when (this) {
    Logger.LogLevel.VERBOSE -> CleverTapAPI.LogLevel.VERBOSE
    Logger.LogLevel.DEBUG -> CleverTapAPI.LogLevel.DEBUG
    Logger.LogLevel.NONE -> CleverTapAPI.LogLevel.OFF
    Logger.LogLevel.INFO,
    Logger.LogLevel.WARN,
    Logger.LogLevel.ERROR -> CleverTapAPI.LogLevel.INFO
}
