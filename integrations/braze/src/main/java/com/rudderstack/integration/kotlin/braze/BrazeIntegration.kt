package com.rudderstack.integration.kotlin.braze

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.braze.Braze
import com.braze.configuration.BrazeConfig
import com.braze.models.outgoing.AttributionData
import com.braze.models.outgoing.BrazeProperties
import com.braze.support.BrazeLogger
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.serialization.json.JsonObject

private const val INSTALL_ATTRIBUTED = "Install Attributed"

private const val ORDER_COMPLETED = "Order Completed"

/**
 * BrazeIntegration is a plugin that sends events to the Braze SDK.
 */
class BrazeIntegration : IntegrationPlugin() {

    override val key: String
        get() = "Braze"

    private var braze: Braze? = null

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
                handleCustomEvent(payload)
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
}

private fun initBraze(application: Application, config: RudderBrazeConfig, logLevel: Logger.LogLevel): Braze {
    with(config) {
        val builder: BrazeConfig.Builder =
            BrazeConfig.Builder()
                .setApiKey(apiKey)
                .setCustomEndpoint(customEndpoint)
        setLogLevel(logLevel)
        Braze.configure(application, builder.build())
        return Braze.getInstance(application).also { braze ->
            application.registerActivityLifecycleCallbacks(braze)
        }
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

private fun Application.registerActivityLifecycleCallbacks(braze: Braze) {
    this.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            // No implementation needed
        }

        override fun onActivityStarted(activity: Activity) {
            braze.openSession(activity)
        }

        override fun onActivityResumed(activity: Activity) {
            // No implementation needed
        }

        override fun onActivityPaused(activity: Activity) {
            // No implementation needed
        }

        override fun onActivityStopped(activity: Activity) {
            braze.closeSession(activity)
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            // No implementation needed
        }

        override fun onActivityDestroyed(activity: Activity) {
            // No implementation needed
        }
    })
}
