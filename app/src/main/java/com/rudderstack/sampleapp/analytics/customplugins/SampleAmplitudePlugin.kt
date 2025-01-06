package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.DestinationPlugin
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class SampleAmplitudePlugin: DestinationPlugin() {

    private var amplitudeSdk: SampleAmplitudeSdk? = null

    override val key: String
        get() = "Amplitude"

    override fun create(destinationConfig: JsonObject, analytics: Analytics, config: Configuration): Any? {
        val apiKey = destinationConfig["apiKey"]?.jsonPrimitive?.content
        return apiKey?.let {
            SampleAmplitudeSdk.create(it)
        }
    }

    override fun onDestinationReady(destination: Any?) {
        amplitudeSdk = destination as? SampleAmplitudeSdk
        LoggerAnalytics.debug("SampleAmplitudePlugin: Destination $key is ready")
    }

    override fun track(event: TrackEvent) {
        val destination = amplitudeSdk
        destination?.track(event.event, event.properties)
    }
}

class SampleAmplitudeSdk private constructor(private val key: String) {

    fun track(event: String, properties: Map<String, Any>) {
        // Track event using Amplitude SDK
        LoggerAnalytics.debug("SampleAmplitudeSdk: track event $event with properties $properties")
    }

    companion object {

        fun create(key: String): SampleAmplitudeSdk {
            // Create Amplitude SDK instance
            return runBlocking {
                // simulate a delay in creation
                delay(1000)
                SampleAmplitudeSdk(key)
            }
        }
    }
}
