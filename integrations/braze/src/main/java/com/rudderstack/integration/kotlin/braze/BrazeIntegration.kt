package com.rudderstack.integration.kotlin.braze

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.braze.Braze
import com.braze.BrazeUser
import com.braze.configuration.BrazeConfig
import com.braze.enums.Gender
import com.braze.enums.Month
import com.braze.models.outgoing.AttributionData
import com.braze.models.outgoing.BrazeProperties
import com.braze.support.BrazeLogger
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.StandardIntegration
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import java.util.Calendar

private const val INSTALL_ATTRIBUTED = "Install Attributed"

private const val ORDER_COMPLETED = "Order Completed"

internal const val ALIAS_LABEL = "rudder_id"
private const val BRAZE_KEY = "Braze"

/**
 * BrazeIntegration is a plugin that sends events to the Braze SDK.
 */
@OptIn(InternalRudderApi::class)
class BrazeIntegration : StandardIntegration, IntegrationPlugin(), ActivityLifecycleObserver {

    override val key: String
        get() = BRAZE_KEY

    private var braze: Braze? = null

    private var previousIdentifyTraits: IdentifyTraits? = null

    private lateinit var brazeConfig: RudderBrazeConfig

    public override fun create(destinationConfig: JsonObject) {
        braze ?: run {
            destinationConfig.parse<RudderBrazeConfig>(analytics.logger)?.let { config ->
                config.logger = analytics.logger
                this.brazeConfig = config
                initBraze(analytics.application, config, analytics.configuration.logLevel).also {
                    braze = it
                    setUserAlias()
                }
                analytics.logger.verbose("BrazeIntegration: Braze SDK initialized")
            }
        }
    }

    /**
     * Sets the alias Id.
     * This is mainly needed for the hybrid mode in order to link the anonymous user activity.
     */
    private fun setUserAlias() {
        analytics.anonymousId?.let {
            this.braze?.currentUser?.addAlias(it, ALIAS_LABEL)
            analytics.logger.verbose("BrazeIntegration: Alias call made with anonymousId: $it")
        }
    }

    override fun update(destinationConfig: JsonObject) {
        destinationConfig.parse<RudderBrazeConfig>(analytics.logger)?.let { updatedConfig ->
            updatedConfig.logger = analytics.logger
            this.brazeConfig = updatedConfig
        }
    }

    override fun getDestinationInstance(): Any? {
        return braze
    }

    override fun track(payload: TrackEvent) {
        if (brazeConfig.isHybridMode()) return

        when (payload.event) {
            INSTALL_ATTRIBUTED -> {
                handleInstallAttributedEvent(payload)
            }

            ORDER_COMPLETED -> {
                handleOrderCompletedEvent(payload)
            }

            else -> {
                handleCustomEvent(payload)
            }
        }
    }

    private fun handleInstallAttributedEvent(payload: TrackEvent) {
        payload.properties.parse<InstallAttributed>(analytics.logger)
            .takeIf { it?.campaign != null }
            ?.let { campaign ->
                this.braze?.currentUser?.setAttributionData(
                    AttributionData(
                        network = campaign.campaign?.source,
                        campaign = campaign.campaign?.name,
                        adGroup = campaign.campaign?.adGroup,
                        creative = campaign.campaign?.adCreative,
                    )
                )
                analytics.logger.verbose("BrazeIntegration: Install Attributed event sent.")
            } ?: run {
            handleCustomEvent(payload)
        }
    }

    private fun handleOrderCompletedEvent(payload: TrackEvent) {
        // Get custom (or non-standard) properties present at the root and product level
        val customProperties: JsonObject = payload.properties.filter(
            rootKeys = StandardProperties.getKeysAsList(),
            productKeys = Product.getKeysAsList(),
        )

        payload.properties.getStandardProperties(analytics.logger).let { standardProperties ->
            val currency = standardProperties.currency

            standardProperties.products
                .filter { it.isNotEmpty() }
                .forEach {
                    this.braze?.logPurchase(
                        productId = it.productId,
                        currencyCode = currency,
                        price = it.price,
                        quantity = it.quantity,
                        properties = initBrazeProperties(customProperties),
                    )
                    analytics.logger.verbose(
                        "BrazeIntegration: Order Completed event sent for " +
                            "product_id: ${it.productId}, currency: $currency, price: ${it.price} " +
                            "and properties $customProperties."
                    )
                }
        }
    }

    private fun handleCustomEvent(payload: TrackEvent) {
        payload.properties.takeIf { it.isNotEmpty() }?.let { properties ->
            this.braze?.logCustomEvent(payload.event, initBrazeProperties(properties))
            analytics.logger.verbose("BrazeIntegration: Custom event ${payload.event} and properties $properties sent.")
        } ?: run {
            this.braze?.logCustomEvent(payload.event)
            analytics.logger.verbose("BrazeIntegration: Custom event ${payload.event} sent.")
        }
    }

    override fun identify(payload: IdentifyEvent) {
        if (brazeConfig.isHybridMode()) return

        payload.toIdentifyTraits(analytics.logger).let { currentIdentifyTraits ->
            val deDupedTraits = this.brazeConfig.takeIf { it.supportDedup }?.let {
                currentIdentifyTraits deDupe previousIdentifyTraits
            } ?: currentIdentifyTraits

            deDupedTraits.getExternalIdOrUserId()?.let { this.braze?.changeUser(it) }
            this.braze?.currentUser?.setTraits(deDupedTraits = deDupedTraits, logger = analytics.logger)

            previousIdentifyTraits = currentIdentifyTraits
        }.also { analytics.logger.verbose("BrazeIntegration: Identify event sent.") }
    }

    override fun flush() {
        this.braze?.requestImmediateDataFlush()
        analytics.logger.verbose("BrazeIntegration: Flush call completed")
    }

    override fun onActivityStarted(activity: Activity) {
        braze?.openSession(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        braze?.closeSession(activity)
    }
}

private fun initBraze(application: Application, config: RudderBrazeConfig, logLevel: Logger.LogLevel,): Braze {
    with(config) {
        val builder: BrazeConfig.Builder =
            initBrazeConfig()
                .setApiKey(resolvedAppIdentifierKey)
                .setCustomEndpoint(customEndpoint)
        setLogLevel(logLevel)
        Braze.configure(application, builder.build())
        return Braze.getInstance(application)
    }
}

@VisibleForTesting
internal fun initBrazeConfig() = BrazeConfig.Builder()

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

private fun BrazeUser.setTraits(deDupedTraits: IdentifyTraits, logger: Logger) {
    with(deDupedTraits.context.traits) {
        birthday?.let { setDateOfBirth(it) }
        email?.let { setEmail(it) }
        firstName?.let { setFirstName(it) }
        lastName?.let { setLastName(it) }
        gender?.let { setGender(it, logger) }
        phone?.let { setPhoneNumber(it) }
        address?.let { setAddress(it) }
    }
    setCustomTraits(deDupedTraits.customTraits, logger)
}

private fun BrazeUser.setDateOfBirth(date: Calendar?) {
    date?.also {
        setDateOfBirth(
            year = it[Calendar.YEAR],
            month = Month.entries[it[Calendar.MONTH]],
            day = it[Calendar.DAY_OF_MONTH],
        )
    }
}

private fun BrazeUser.setGender(gender: String?, logger: Logger) {
    when (gender?.uppercase()) {
        "M", "MALE" -> setGender(Gender.MALE)
        "F", "FEMALE" -> setGender(Gender.FEMALE)

        else -> {
            logger.error("BrazeIntegration: Unsupported gender: $gender")
        }
    }
}

private fun BrazeUser.setAddress(address: Address?) {
    setHomeCity(address?.city)
    setCountry(address?.country)
}

private fun BrazeUser.setCustomTraits(customTraits: JsonObject, logger: Logger) {
    customTraits.forEach { (key, value) ->
        when {
            value !is JsonPrimitive -> logUnsupportedType(key, value, logger)
            value.booleanOrNull != null -> setCustomUserAttribute(key, value.boolean)
            value.longOrNull != null -> setCustomUserAttribute(key, value.long)
            value.doubleOrNull != null -> setCustomUserAttribute(key, value.double)
            value.isString -> handleStringValue(key, value.content)
            else -> logUnsupportedType(key, value, logger)
        }
    }
}

private fun BrazeUser.handleStringValue(key: String, value: String) {
    tryDateConversion(value)?.let { seconds ->
        setCustomUserAttributeToSecondsFromEpoch(key, seconds)
    } ?: setCustomUserAttribute(key, value)
}

@VisibleForTesting
internal fun initBrazeProperties(properties: JsonObject): BrazeProperties = BrazeProperties(properties.toJSONObject())
