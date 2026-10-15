package com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils

import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.StandardIntegration
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.serialization.json.JsonObject

class MockLiteIntegrationPlugin(private val appInstance: MockDestinationSdk) : StandardIntegration, IntegrationPlugin() {

    internal var createCount = 0
        private set
    internal var updateCount = 0
        private set
    internal var createError: Exception? = null

    override val key: String
        get() = "MockDestination"

    override fun create(destinationConfig: JsonObject) {
        createCount++
        createError?.let { throw it }
    }

    override fun update(destinationConfig: JsonObject) {
        updateCount++
    }

    override fun getDestinationInstance(): Any = appInstance

    override fun track(payload: TrackEvent) {
        appInstance.trackEvent(payload.event)
    }
}
