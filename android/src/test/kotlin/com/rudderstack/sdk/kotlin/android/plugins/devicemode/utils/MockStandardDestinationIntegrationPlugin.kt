package com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils

import androidx.annotation.VisibleForTesting
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.StandardIntegration
import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class MockStandardDestinationIntegrationPlugin : StandardIntegration, IntegrationPlugin() {

    private var mockDestinationSdk: MockDestinationSdk? = null
    private var previousApiKey = String.empty()
    @VisibleForTesting
    internal var destinationConfig = emptyJsonObject

    override val key: String
        get() = "MockDestination"

    override fun create(destinationConfig: JsonObject) {
        if (mockDestinationSdk == null) {
            this.destinationConfig = destinationConfig
            val apiKey = destinationConfig["apiKey"]?.jsonPrimitive?.content
            apiKey?.let {
                previousApiKey = it
                mockDestinationSdk = initialiseMockSdk(it)
            }
        }
    }

    override fun update(destinationConfig: JsonObject) {
        this.destinationConfig = destinationConfig
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
