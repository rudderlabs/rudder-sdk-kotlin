package com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils

import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.serialization.json.JsonObject

class MockCustomIntegrationPlugin : IntegrationPlugin() {

    private var mockDestinationSdk: MockDestinationSdk? = null

    override val key: String
        get() = "MockDestination"

    override fun create(destinationConfig: JsonObject) {
        if (mockDestinationSdk == null) {
            val apiKey = "MyApiKey"
            mockDestinationSdk = initialiseMockSdk(apiKey)
        }
    }

    internal fun initialiseMockSdk(apiKey: String): MockDestinationSdk {
        return MockDestinationSdk.initialise(apiKey)
    }

    override fun getDestinationInstance(): Any? {
        return mockDestinationSdk
    }

    override fun track(payload: TrackEvent) {
        val destination = mockDestinationSdk
        destination?.trackEvent(payload.event)
    }

    override fun screen(payload: ScreenEvent) {
        val destination = mockDestinationSdk
        destination?.screenEvent(payload.screenName)
    }

    override fun group(payload: GroupEvent) {
        val destination = mockDestinationSdk
        destination?.groupEvent(payload.groupId)
    }

    override fun identify(payload: IdentifyEvent) {
        val destination = mockDestinationSdk
        destination?.identifyUser(payload.userId)
    }

    override fun alias(payload: AliasEvent) {
        val destination = mockDestinationSdk
        destination?.aliasUser(payload.userId, payload.previousId)
    }

    override fun reset() {
        mockDestinationSdk?.reset()
    }

    override fun flush() {
        mockDestinationSdk?.flush()
    }
}
