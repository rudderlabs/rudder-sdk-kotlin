package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class SampleIntegrationPlugin : IntegrationPlugin() {

    private var destinationSdk: SampleDestinationSdk? = null

    override val key: String
        get() = "Amplitude"

    override fun create(
        destinationConfig: JsonObject,
    ) {
        // Update destination instance if needed
        if (destinationSdk == null) {
            val apiKey = destinationConfig["apiKey"]?.jsonPrimitive?.content
            apiKey?.let {
                destinationSdk = SampleDestinationSdk.create(it)
            }
        }
    }

    override fun getDestinationInstance(): Any? {
        return destinationSdk
    }

    override fun track(payload: TrackEvent): Event {
        LoggerAnalytics.debug("SampleIntegrationPlugin: track event $payload")
        LoggerAnalytics.debug("SampleIntegrationPlugin: destinationConfig $destinationConfig")
        destinationSdk?.track(payload.event, payload.properties)
        return payload
    }

    override fun screen(payload: ScreenEvent): Event? {
        LoggerAnalytics.debug("SampleIntegrationPlugin: screen event $payload")
        return super.screen(payload)
    }

    override fun flush() {
        LoggerAnalytics.debug("SampleIntegrationPlugin: flush")
        super.flush()
    }

    override fun reset() {
        LoggerAnalytics.debug("SampleIntegrationPlugin: reset")
        super.reset()
    }
}

class SampleDestinationSdk private constructor(private val key: String) {

    fun track(event: String, properties: Map<String, Any>) {
        // Track event using Amplitude SDK
        LoggerAnalytics.debug("SampleAmplitudeSdk: track event $event with properties $properties")
    }

    companion object {

        fun create(key: String): SampleDestinationSdk {
            // Create Amplitude SDK instance
            return runBlocking {
                // simulate a delay in creation
                delay(6000)
                SampleDestinationSdk(key)
            }
        }
    }
}
