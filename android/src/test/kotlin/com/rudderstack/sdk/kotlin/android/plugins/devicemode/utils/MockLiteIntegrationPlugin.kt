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
    internal var onCreate: (() -> Unit)? = null
    internal var instanceError: Exception? = null

    override val key: String
        get() = "MockDestination"

    override fun create(destinationConfig: JsonObject) {
        createCount++
        onCreate?.invoke()
        createError?.let { throw it }
    }

    override fun update(destinationConfig: JsonObject) {
        updateCount++
    }

    override fun getDestinationInstance(): Any = instanceError?.let { throw it } ?: appInstance

    override fun track(payload: TrackEvent) {
        appInstance.trackEvent(payload.event)
    }
}
