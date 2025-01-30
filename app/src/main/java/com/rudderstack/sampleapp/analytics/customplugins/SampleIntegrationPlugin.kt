package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
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
        // use the destinationConfig to update the way track call is made to destinationSdk
        LoggerAnalytics.debug("SampleIntegrationPlugin: destinationConfig $destinationConfig")

        destinationSdk?.track(payload.event, payload.properties)
        return payload
    }

    override fun flush() {
        destinationSdk?.flush()
    }

    override fun reset() {
        destinationSdk?.reset()
    }
}

class SampleDestinationSdk private constructor(private val key: String) {

    fun track(event: String, properties: Map<String, Any>) {
        LoggerAnalytics.debug("SampleAmplitudeSdk: track event $event with properties $properties")
    }

    fun flush() {
        LoggerAnalytics.debug("SampleAmplitudeSdk: flush")
    }

    fun reset() {
        LoggerAnalytics.debug("SampleAmplitudeSdk: reset")
    }

    companion object {

        fun create(key: String): SampleDestinationSdk {
            // Create Amplitude SDK instance
            return runBlocking {
                // simulate a delay in creation
                delay(1000)
                SampleDestinationSdk(key)
            }
        }
    }
}
