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

    override fun create(destinationConfig: JsonObject) {
        if (mockDestinationSdk == null) {
            val apiKey = destinationConfig["apiKey"]?.jsonPrimitive?.content
            apiKey?.let {
                previousApiKey = it
                mockDestinationSdk = initialiseMockSdk(it)
            }
        }
    }

    internal fun initialiseMockSdk(apiKey: String): MockDestinationSdk {
        return MockDestinationSdk.initialise(apiKey)
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
