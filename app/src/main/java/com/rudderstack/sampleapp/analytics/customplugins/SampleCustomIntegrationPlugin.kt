package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject

/**
 * Sample custom integration plugin.
 *
 * This is a sample custom integration plugin that demonstrates how to create a custom
 * integration plugin for RudderStack.
 * It implements the [IntegrationPlugin] class and overrides the required methods.
 *
 * To use it, simply add it to the `Analytics` instance of your app using [add] method.
 */
class SampleCustomIntegrationPlugin : IntegrationPlugin() {

    private var destinationSdk: SampleDestinationSdk? = null

    override val key: String
        get() = "MyKey"

    /**
     * For custom integration plugins, the [destinationConfig] is an empty [JsonObject], so it is not used.
     */
    override fun create(
        destinationConfig: JsonObject,
    ) {
        if (destinationSdk == null) {
            val apiKey = "SomeCustomApiKey"
            destinationSdk = SampleDestinationSdk.create(apiKey)
        }
    }

    override fun getDestinationInstance(): Any? {
        return destinationSdk
    }

    override fun track(payload: TrackEvent) {
        destinationSdk?.track(payload.event, payload.properties)
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
