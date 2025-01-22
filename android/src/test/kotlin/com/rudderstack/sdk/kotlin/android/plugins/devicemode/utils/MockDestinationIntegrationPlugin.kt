package com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils

import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class MockDestinationIntegrationPlugin : IntegrationPlugin() {

    private var mockDestinationSdk: MockDestinationSdk? = null
    private var previousApiKey = String.empty()

    override val key: String
        get() = "MockDestination"

    override fun create(destinationConfig: JsonObject): Boolean {
        try {
            val apiKey = destinationConfig["apiKey"]?.jsonPrimitive?.content
            apiKey?.let {
                previousApiKey = it
                mockDestinationSdk = initialiseMockSdk(it)
                return true
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    internal fun initialiseMockSdk(apiKey: String): MockDestinationSdk {
        return MockDestinationSdk.initialise(apiKey)
    }

    override fun update(destinationConfig: JsonObject): Boolean {
        // this is a simulated version of how to update the destination
        val apiKey = destinationConfig["apiKey"]?.jsonPrimitive?.content
        // destination SDK is reinitialised with the new API key if it is null or API key is different.
        if (mockDestinationSdk == null || apiKey != previousApiKey) {
            return create(destinationConfig)
        }
        // if the above check is passed then the destination is already in ready state, so return true.
        return true
    }

    override fun getDestinationInstance(): Any? {
        return mockDestinationSdk
    }

    override fun track(payload: TrackEvent): Event {
        val destination = mockDestinationSdk
        destination?.trackEvent(payload.event)
        return payload
    }

    override fun screen(payload: ScreenEvent): Event {
        val destination = mockDestinationSdk
        destination?.screenEvent(payload.screenName)
        return payload
    }

    override fun group(payload: GroupEvent): Event {
        val destination = mockDestinationSdk
        destination?.groupEvent(payload.groupId)
        return payload
    }

    override fun identify(payload: IdentifyEvent): Event {
        val destination = mockDestinationSdk
        destination?.identifyUser(payload.userId)
        return payload
    }

    override fun alias(payload: AliasEvent): Event {
        val destination = mockDestinationSdk
        destination?.aliasUser(payload.userId, payload.previousId)
        return payload
    }

    override fun reset() {
        mockDestinationSdk?.reset()
    }

    override fun flush() {
        mockDestinationSdk?.flush()
    }
}
