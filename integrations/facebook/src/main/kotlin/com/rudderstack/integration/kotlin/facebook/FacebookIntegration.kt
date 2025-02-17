package com.rudderstack.integration.kotlin.facebook

import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.android.utils.getInt
import com.rudderstack.sdk.kotlin.android.utils.getString
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.math.BigDecimal
import java.util.Currency

internal const val FACEBOOK_KEY = "Facebook App Events"
private const val MAX_EVENT_LENGTH = 40
private const val MAX_REMAINING_SCREEN_EVENT_LENGTH = 26

/**
 * The Facebook Integration Plugin. This plugin is used to send events to Facebook.
 */
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
                            config.country,
                            config.state
                        )
                        LoggerAnalytics.debug(
                            "FacebookIntegration: Data Processing Options set " +
                                "to LDU with country: ${config.country} and state: ${config.state}"
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
            .take(MAX_REMAINING_SCREEN_EVENT_LENGTH)
            .takeIf {
                it.isNotEmpty()
            }?.let {
                if (payload.properties.isNotEmpty()) {
                    val params = Bundle()
                    handleCustomScreenProperties(payload.properties, params)
                    facebookAppEventsLogger?.logEvent("Viewed $it Screen", params)
                } else {
                    facebookAppEventsLogger?.logEvent("Viewed $it Screen")
                }
                LoggerAnalytics.debug("FacebookIntegration: Screen $it sent to Facebook")
            }

        return payload
    }

    override fun identify(payload: IdentifyEvent): Event? {
        AppEventsLogger.setUserID(payload.userId)

        val address = analytics.traits?.get("address")?.let {
            LenientJson.decodeFromJsonElement<Address>(it)
        }

        AppEventsLogger.setUserData(
            email = analytics.traits?.getString("email"),
            firstName = analytics.traits?.getString("firstName"),
            lastName = analytics.traits?.getString("lastName"),
            phone = analytics.traits?.getString("phone"),
            dateOfBirth = analytics.traits?.getString("dateOfBirth"),
            gender = analytics.traits?.getString("gender"),
            city = address?.city,
            state = address?.state,
            zip = address?.postalCode,
            country = address?.country
        )

        return payload
    }

    override fun track(payload: TrackEvent): Event? {
        payload.event
            .take(MAX_EVENT_LENGTH)
            .let {
                FACEBOOK_EVENTS_MAPPING[it] ?: it
            }
            .let { eventName ->
                val params = Bundle()
                handleCustomTrackProperties(payload.properties, params)
                when (eventName) {
                    // Standard events
                    AppEventsConstants.EVENT_NAME_ADDED_TO_CART,
                    AppEventsConstants.EVENT_NAME_ADDED_TO_WISHLIST,
                    AppEventsConstants.EVENT_NAME_VIEWED_CONTENT -> {
                        handleStandardTrackProperties(payload.properties, params, eventName)
                        getValueToSum(payload.properties, PRICE)?.let { price ->
                            facebookAppEventsLogger?.logEvent(eventName, price, params)
                        }
                    }

                    AppEventsConstants.EVENT_NAME_INITIATED_CHECKOUT,
                    AppEventsConstants.EVENT_NAME_SPENT_CREDITS -> {
                        handleStandardTrackProperties(payload.properties, params, eventName)
                        getValueToSum(payload.properties, VALUE)?.let { value ->
                            facebookAppEventsLogger?.logEvent(eventName, value, params)
                        }
                    }

                    ECommerceEvents.ORDER_COMPLETED -> {
                        handleStandardTrackProperties(payload.properties, params, eventName)
                        val revenue = getRevenue(payload.properties)
                        val currency = getCurrency(payload.properties)
                        revenue?.let {
                            facebookAppEventsLogger?.logPurchase(
                                BigDecimal.valueOf(revenue),
                                Currency.getInstance(currency),
                                params
                            )
                        }
                    }

                    AppEventsConstants.EVENT_NAME_SEARCHED,
                    AppEventsConstants.EVENT_NAME_ADDED_PAYMENT_INFO,
                    AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION,
                    AppEventsConstants.EVENT_NAME_ACHIEVED_LEVEL,
                    AppEventsConstants.EVENT_NAME_COMPLETED_TUTORIAL,
                    AppEventsConstants.EVENT_NAME_UNLOCKED_ACHIEVEMENT,
                    AppEventsConstants.EVENT_NAME_SUBSCRIBE,
                    AppEventsConstants.EVENT_NAME_START_TRIAL,
                    AppEventsConstants.EVENT_NAME_AD_CLICK,
                    AppEventsConstants.EVENT_NAME_AD_IMPRESSION,
                    AppEventsConstants.EVENT_NAME_RATED -> {
                        handleStandardTrackProperties(payload.properties, params, eventName)
                        facebookAppEventsLogger?.logEvent(eventName, params)
                    }

                    else -> facebookAppEventsLogger?.logEvent(eventName, params)
                }
                LoggerAnalytics.debug("FacebookIntegration: Event $eventName sent to Facebook")
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

    private fun handleStandardTrackProperties(properties: JsonObject, params: Bundle, eventName: String) {
        properties.getString(PRODUCT_ID)?.let {
            params.putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, it)
        }

        properties.getInt(RATING)?.let {
            params.putInt(AppEventsConstants.EVENT_PARAM_MAX_RATING_VALUE, it)
        }

        properties.getString(PROMOTION_NAME)?.let {
            params.putString(AppEventsConstants.EVENT_PARAM_AD_TYPE, it)
        }

        properties.getString(ORDER_ID)?.let {
            params.putString(AppEventsConstants.EVENT_PARAM_ORDER_ID, it)
        }

        if (eventName != ECommerceEvents.ORDER_COMPLETED) {
            params.putString(AppEventsConstants.EVENT_PARAM_CURRENCY, getCurrency(properties))
        }

        val description = properties[DESCRIPTION]
        if (description != null) {
            params.putString(AppEventsConstants.EVENT_PARAM_DESCRIPTION, description.toString())
        }

        val query = properties[QUERY]
        if (query != null) {
            params.putString(AppEventsConstants.EVENT_PARAM_SEARCH_STRING, query.toString())
        }
    }

    private fun handleCustomTrackProperties(properties: JsonObject, params: Bundle) {
        addPropertiesToBundle(properties, params, false)
    }

    private fun handleCustomScreenProperties(properties: JsonObject, params: Bundle) {
        addPropertiesToBundle(properties, params, true)
    }
}
