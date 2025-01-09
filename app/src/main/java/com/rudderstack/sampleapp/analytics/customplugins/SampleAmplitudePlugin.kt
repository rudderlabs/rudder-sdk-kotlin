package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.DestinationPlugin
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class SampleAmplitudePlugin: DestinationPlugin() {

    private var amplitudeSdk: SampleAmplitudeSdk? = null

    override val key: String
        get() = "Amplitude"

    override fun create(destinationConfig: JsonObject, analytics: Analytics, config: Configuration): Boolean {
        try {
            val apiKey = destinationConfig["apiKey"]?.jsonPrimitive?.content
            apiKey?.let {
                amplitudeSdk = SampleAmplitudeSdk.create(it)
                return true
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    override fun getUnderlyingInstance(): Any? {
        return amplitudeSdk
    }

    override fun track(payload: TrackEvent): Event {
        val destination = amplitudeSdk
        destination?.track(payload.event, payload.properties)
        return payload
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
