package com.rudderstack.integration.kotlin.braze

import com.braze.Braze
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.serialization.json.JsonObject

/**
 * BrazeIntegration is a plugin that sends events to the Braze SDK.
 */
class BrazeIntegration : IntegrationPlugin() {
    override val key: String
        get() = "Braze"

    private var braze: Braze? = null

    public override fun create(destinationConfig: JsonObject) {
        braze ?: run {
            destinationConfig.parseConfig<BrazeConfig>().let { config ->

                LoggerAnalytics.verbose("BrazeIntegration: Adjust SDK initialized. $config")
            }
        }
    }

    override fun getDestinationInstance(): Any? {
        return braze
    }

    override fun track(payload: TrackEvent): Event {
        println("BrazeIntegration: track called with payload: $payload")

        return payload
    }
}
