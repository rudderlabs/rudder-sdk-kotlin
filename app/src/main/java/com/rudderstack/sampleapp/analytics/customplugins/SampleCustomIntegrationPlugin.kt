package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject

/**
 * Sample custom integration plugin.
 *
 * This is a sample custom integration plugin that demonstrates how to create a custom
 * integration plugin for RudderStack.
 * It implements the [IntegrationPlugin] class and overrides the required methods.
 *
 * To use it, simply add it to the `Analytics` instance of your app using [add] method.
 */
class SampleCustomIntegrationPlugin : IntegrationPlugin() {

    private var destinationSdk: SampleDestinationSdk? = null

    override val key: String
        get() = "MyKey"

    /**
     * For custom integration plugins, the [destinationConfig] is an empty [JsonObject], so it is not used.
     */
    override fun create(
        destinationConfig: JsonObject,
    ) {
        if (destinationSdk == null) {
            val apiKey = "SomeCustomApiKey"
            destinationSdk = SampleDestinationSdk.create(apiKey)
        }
    }

    override fun getDestinationInstance(): Any? {
        return destinationSdk
    }

    override fun track(payload: TrackEvent) {
        destinationSdk?.track(payload.event, payload.properties)
    }

    override fun screen(payload: ScreenEvent) {
        destinationSdk?.screen(payload.screenName, payload.properties)
    }

    override fun group(payload: GroupEvent) {
        destinationSdk?.group(payload.groupId, payload.traits)
    }

    override fun identify(payload: IdentifyEvent) {
        destinationSdk?.identifyUser(payload.userId, analytics.traits ?: emptyMap())
    }

    override fun alias(payload: AliasEvent) {
        destinationSdk?.aliasUser(payload.userId, payload.previousId)
    }

    override fun flush() {
        destinationSdk?.flush()
    }

    override fun reset() {
        destinationSdk?.reset()
    }
}

class SampleDestinationSdk private constructor(private val key: String) {

    fun track(event: String, properties: Map<String, Any>) {
        LoggerAnalytics.debug("SampleDestinationSdk: track event $event with properties $properties")
    }

    fun screen(screenName: String, properties: Map<String, Any>) {
        LoggerAnalytics.debug("SampleDestinationSdk: screen event $screenName with properties $properties")
    }

    fun group(groupId: String, traits: Map<String, Any>) {
        LoggerAnalytics.debug("SampleDestinationSdk: group event $groupId with traits $traits")
    }

    fun identifyUser(userId: String, traits: Map<String, Any>) {
        LoggerAnalytics.debug("SampleDestinationSdk: identify user $userId with traits $traits")
    }

    fun aliasUser(userId: String, previousId: String) {
        LoggerAnalytics.debug("SampleDestinationSdk: alias user $userId with previous ID $previousId")
    }

    fun flush() {
        LoggerAnalytics.debug("SampleDestinationSdk: flush")
    }

    fun reset() {
        LoggerAnalytics.debug("SampleDestinationSdk: reset")
    }

    companion object {

        fun create(key: String): SampleDestinationSdk {
            // Create SampleDestinationSdk SDK instance
            return runBlocking {
                // simulate a delay in creation
                delay(1000)
                SampleDestinationSdk(key)
            }
        }
    }
}
