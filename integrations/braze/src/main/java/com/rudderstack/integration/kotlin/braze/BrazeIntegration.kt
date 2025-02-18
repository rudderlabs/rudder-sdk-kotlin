package com.rudderstack.integration.kotlin.braze

import android.app.Activity
import android.app.Application
import android.util.Log
import com.braze.Braze
import com.braze.BrazeUser
import com.braze.configuration.BrazeConfig
import com.braze.enums.Gender
import com.braze.enums.Month
import com.braze.models.outgoing.AttributionData
import com.braze.models.outgoing.BrazeProperties
import com.braze.support.BrazeLogger
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import java.util.Calendar

private const val INSTALL_ATTRIBUTED = "Install Attributed"

private const val ORDER_COMPLETED = "Order Completed"

/**
 * BrazeIntegration is a plugin that sends events to the Braze SDK.
 */
@OptIn(InternalRudderApi::class)
class BrazeIntegration : IntegrationPlugin(), ActivityLifecycleObserver {

    override val key: String
        get() = "Braze"

    private var braze: Braze? = null

    private var previousIdentifyTraits: IdentifyTraits? = null

    // TODO("Add the way to update this value dynamically through `update` method.")
    private lateinit var brazeConfig: RudderBrazeConfig

    public override fun create(destinationConfig: JsonObject) {
        braze ?: run {
            destinationConfig.parse<RudderBrazeConfig>().let { config ->
                this.brazeConfig = config
                initBraze(analytics.application, config, analytics.configuration.logLevel).also {
                    braze = it
                }
                // TODO("Address hybrid mode issue by making Alias call with anonymousId")
                LoggerAnalytics.verbose("BrazeIntegration: Adjust SDK initialized. $config")
            }
        }
    }

    override fun getDestinationInstance(): Any? {
        return braze
    }

    override fun track(payload: TrackEvent): Event {
        // TODO("Handle hybrid mode")
        when (payload.event) {
            INSTALL_ATTRIBUTED -> {
                handleInstallAttributedEvent(payload)
            }

            ORDER_COMPLETED -> {
                handleOrderCompletedEvent(payload)
            }

            // Custom event
            else -> {
                handleCustomEvent(payload)
            }
        }
        return payload
    }

    private fun handleInstallAttributedEvent(payload: TrackEvent) {
        payload.properties.parse<InstallAttributed>()
            .takeIf { it.campaign != null }
            ?.let { campaign ->
                this.braze?.currentUser?.setAttributionData(
                    AttributionData(
                        network = campaign.campaign?.source,
                        campaign = campaign.campaign?.name,
                        adGroup = campaign.campaign?.adGroup,
                        creative = campaign.campaign?.adCreative,
                    )
                )
                LoggerAnalytics.verbose("BrazeIntegration: Install Attributed event sent.")
            } ?: run {
            handleCustomEvent(payload)
        }
    }

    private fun handleOrderCompletedEvent(payload: TrackEvent) {
        // Get custom (or non-standard) properties present at the root and product level
        val customProperties: JsonObject = payload.properties.getCustomProperties()

        payload.properties.getStandardProperties().let { standardProperties ->
            val currency = standardProperties.currency

            standardProperties.products.takeIf { it.isNotEmpty() }?.forEach {
                this.braze?.logPurchase(
                    productId = it.productId,
                    currencyCode = currency,
                    price = it.price,
                    properties = BrazeProperties(customProperties)
                )
                LoggerAnalytics.verbose(
                    "BrazeIntegration: Order Completed event sent for " +
                        "product_id: ${it.productId}, currency: $currency, price: ${it.price} " +
                        "and properties $customProperties."
                )
            } ?: run {
                LoggerAnalytics.error("BrazeIntegration: Order Completed event not sent as products are empty.")
            }
        }
    }

    private fun handleCustomEvent(payload: TrackEvent) {
        payload.properties.takeIf { it.isNotEmpty() }?.let { properties ->
            this.braze?.logCustomEvent(payload.event, BrazeProperties(properties))
            LoggerAnalytics.verbose("BrazeIntegration: Custom event ${payload.event} and properties $properties sent.")
        } ?: run {
            this.braze?.logCustomEvent(payload.event)
            LoggerAnalytics.verbose("BrazeIntegration: Custom event ${payload.event} sent.")
        }
    }

    override fun identify(payload: IdentifyEvent): Event {
        payload.getIdentifyTraits().let { currentTraits ->
            val deDupedTraits = this.brazeConfig.takeIf { it.supportDedup }?.let {
                getDeDupedIdentifyTraits(currentTraits = currentTraits, previousTraits = previousIdentifyTraits)
            } ?: currentTraits

            // TODO: Check if their API accepts null or not!
            this.braze?.changeUser(deDupedTraits.getExternalIdOrUserId())
            this.braze?.currentUser?.apply {
                setDate(deDupedTraits.context.traits.birthday)
                setEmail(deDupedTraits.context.traits.email)
                setFirstName(deDupedTraits.context.traits.firstName)
                setLastName(deDupedTraits.context.traits.lastName)
                setGender(deDupedTraits.context.traits.gender)
                setPhoneNumber(deDupedTraits.context.traits.phone)
                setAddress(deDupedTraits.context.traits.address)

                setCustomTraits(payload.traits?.filterKeys(filterKeys = TraitsMatcher.standardTraitKeys))
            }

            previousIdentifyTraits = currentTraits
        }.also { LoggerAnalytics.verbose("BrazeIntegration: Identify event sent.") }
        return payload
    }

    private fun BrazeUser.setDate(date: Calendar?) {
        date?.also {
            setDateOfBirth(
                year = it[Calendar.YEAR],
                month = Month.entries[it[Calendar.MONTH]],
                day = it[Calendar.DAY_OF_MONTH],
            )
        }
    }

    private fun BrazeUser.setGender(gender: String?) {
        when (gender?.uppercase()) {
            "M", "MALE" -> setGender(Gender.MALE)
            "F", "FEMALE" -> setGender(Gender.FEMALE)

            else -> {
                LoggerAnalytics.error("BrazeIntegration: Unsupported gender: $gender")
            }
        }
    }

    private fun BrazeUser.setAddress(address: Address?) {
        setHomeCity(address?.city)
        setCountry(address?.country)
    }

    private fun BrazeUser.setCustomTraits(traits: JsonObject?) {
        traits?.forEach { (key, value) ->
            when {
                value !is JsonPrimitive -> logUnsupportedType(key, value)
                value.booleanOrNull != null -> setCustomUserAttribute(key, value.boolean)
                value.intOrNull != null -> setCustomUserAttribute(key, value.int)
                value.doubleOrNull != null -> setCustomUserAttribute(key, value.double)
                value.floatOrNull != null -> setCustomUserAttribute(key, value.float)
                value.longOrNull != null -> setCustomUserAttribute(key, value.long)
                value.isString -> handleStringValue(key, value.content)
                else -> logUnsupportedType(key, value)
            }
        }
    }

    private fun BrazeUser.handleStringValue(key: String, content: String) {
        tryDateConversion(content)?.let { seconds ->
            setCustomUserAttributeToSecondsFromEpoch(key, seconds)
        } ?: setCustomUserAttribute(key, content)
    }

    override fun onActivityStarted(activity: Activity) {
        braze?.openSession(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        braze?.closeSession(activity)
    }
}

private fun initBraze(application: Application, config: RudderBrazeConfig, logLevel: Logger.LogLevel): Braze {
    with(config) {
        val builder: BrazeConfig.Builder =
            BrazeConfig.Builder()
                .setApiKey(apiKey)
                .setCustomEndpoint(customEndpoint)
        setLogLevel(logLevel)
        Braze.configure(application, builder.build())
        return Braze.getInstance(application)
    }
}

private fun setLogLevel(rudderLogLevel: Logger.LogLevel) {
    when (rudderLogLevel) {
        Logger.LogLevel.VERBOSE -> Log.VERBOSE
        Logger.LogLevel.DEBUG -> Log.DEBUG
        Logger.LogLevel.INFO -> Log.INFO
        Logger.LogLevel.WARN -> Log.WARN
        Logger.LogLevel.ERROR -> Log.ERROR
        Logger.LogLevel.NONE -> BrazeLogger.SUPPRESS
    }.also {
        BrazeLogger.logLevel = it
    }
}
