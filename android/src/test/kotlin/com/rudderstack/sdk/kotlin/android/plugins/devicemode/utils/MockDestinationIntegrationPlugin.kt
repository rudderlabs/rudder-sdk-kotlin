package com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class MockDestinationIntegrationPlugin : IntegrationPlugin() {

    private var mockDestinationSdk: MockDestinationSdk? = null

    override val key: String
        get() = "MockDestination"

    override fun create(destinationConfig: JsonObject, analytics: Analytics, config: Configuration): Boolean {
        try {
            val apiKey = destinationConfig["apiKey"]?.jsonPrimitive?.content
            apiKey?.let {
                mockDestinationSdk = MockDestinationSdk.initialise(it)
                return true
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    override fun update(destinationConfig: JsonObject): Boolean {
        // this is a simulated version of how to update the destination
        // destination SDK is reinitialised with the new API key and then update method of SDK is called.
        val apiKey = destinationConfig["apiKey"]?.jsonPrimitive?.content
        apiKey?.let {
            mockDestinationSdk = MockDestinationSdk.initialise(it)
        }

        return mockDestinationSdk?.let {
            it.update()
            true
        } ?: run {
            false
        }
    }

    override fun getUnderlyingInstance(): Any? {
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
