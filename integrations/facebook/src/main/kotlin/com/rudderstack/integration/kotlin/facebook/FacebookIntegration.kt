package com.rudderstack.integration.kotlin.facebook

import androidx.annotation.VisibleForTesting
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.facebook.appevents.AppEventsLogger
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.android.utils.getString
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

internal const val FACEBOOK_KEY = "Facebook App Events"

class FacebookIntegration : IntegrationPlugin() {

    private var facebookAppEventsLogger: AppEventsLogger? = null

    override val key: String
        get() = FACEBOOK_KEY

    override fun create(destinationConfig: JsonObject) {
        facebookAppEventsLogger ?: run {
            destinationConfig.parseConfig<FacebookDestinationConfig>()
                .let { config ->
                    if (analytics.configuration.logLevel >= Logger.LogLevel.DEBUG) {
                        FacebookSdk.setIsDebugEnabled(true)
                        FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
                    }

                    if (config.limitedDataUse) {
                        FacebookSdk.setDataProcessingOptions(
                            arrayOf("LDU"),
                            config.dpoCountry,
                            config.dpoState
                        )
                        LoggerAnalytics.debug(
                            "FacebookIntegration: Data Processing Options set " +
                                "to LDU with country: ${config.dpoCountry} and state: ${config.dpoState}"
                        )
                    } else {
                        FacebookSdk.setDataProcessingOptions(arrayOf())
                        LoggerAnalytics.debug("FacebookIntegration: Data Processing Options cleared")
                    }
                    AppEventsLogger.activateApp(analytics.application, config.appId)
                    facebookAppEventsLogger = provideAppEventsLogger()
                }
        }
    }

    @VisibleForTesting
    internal fun provideAppEventsLogger(): AppEventsLogger {
        return AppEventsLogger.newLogger(analytics.application)
    }

    override fun screen(payload: ScreenEvent): Event? {
        payload.screenName
            .take(26)
            .takeIf {
                it.isNotEmpty()
            }?.let {
                facebookAppEventsLogger?.logEvent("Viewed $it Screen")
            }

        return payload
    }

    override fun identify(payload: IdentifyEvent): Event? {
        AppEventsLogger.setUserID(payload.userId)

        analytics.traits?.get("address")?.let {
            val address = LenientJson.decodeFromJsonElement<Address>(it)

            AppEventsLogger.setUserData(
                email = analytics.traits?.get("email")?.getString(),
                firstName = analytics.traits?.get("firstName")?.getString(),
                lastName = analytics.traits?.get("lastName")?.getString(),
                phone = analytics.traits?.get("phone")?.getString(),
                dateOfBirth = analytics.traits?.get("dateOfBirth")?.getString(),
                gender = analytics.traits?.get("gender")?.getString(),
                city = address.city,
                state = address.state,
                zip = address.postalCode,
                country = address.country
            )
        }

        return payload
    }

    override fun track(payload: TrackEvent): Event? {
        payload.event.take(40).let {
            facebookAppEventsLogger?.logEvent(it)
        }
        return payload
    }

    override fun reset() {
        AppEventsLogger.clearUserID()
        AppEventsLogger.clearUserData()
    }

    override fun getDestinationInstance(): Any? {
        return facebookAppEventsLogger
    }
}
