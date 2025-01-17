package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class SampleIntegrationPlugin: IntegrationPlugin() {

    private var destinationSdk: SampleDestinationSdk? = null

    override val key: String
        get() = "Amplitude"

    override fun create(destinationConfig: JsonObject, analytics: Analytics, config: Configuration): Boolean {
        try {
            val apiKey = destinationConfig["apiKey"]?.jsonPrimitive?.content
            apiKey?.let {
                destinationSdk = SampleDestinationSdk.create(it)
                return true
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    override fun update(destinationConfig: JsonObject): Boolean {
        // Update destination instance if needed
        val apiKey = destinationConfig["apiKey"]?.jsonPrimitive?.content
        return apiKey?.let {
            destinationSdk = SampleDestinationSdk.create(it)
            true
        } ?: run {
            false
        }
    }

    override fun getUnderlyingInstance(): Any? {
        return destinationSdk
    }

    override fun track(payload: TrackEvent): Event {
        val destination = destinationSdk
        destination?.track(payload.event, payload.properties)
        return payload
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
                delay(1000)
                SampleDestinationSdk(key)
            }
        }
    }
}
