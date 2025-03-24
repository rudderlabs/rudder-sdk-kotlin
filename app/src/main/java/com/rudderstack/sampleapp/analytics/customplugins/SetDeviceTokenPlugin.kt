package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement

/**
 * A plugin that sets the device token in the event payload.
 *
 * Add this plugin just after the SDK initialization to set the device token in the event payload.
 *
 * Add the plugin like this:
 * ```
 * analytics.add(SetDeviceTokenPlugin(deviceToken = "someDeviceToken"))
 * ```
 *
 * @param deviceToken The device token to be set in the event payload.
 */
class SetDeviceTokenPlugin(
    private val deviceToken: String
) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    override suspend fun intercept(event: Event): Event {
        setDeviceToken(event)
        LoggerAnalytics.verbose("SetDeviceTokenPlugin: Setting device token: $deviceToken in the event payload")
        return event
    }

    private fun setDeviceToken(event: Event): Event {
        val device = event.context["device"] as? JsonObject ?: JsonObject(emptyMap())

        val updatedDevice = JsonObject(
            device.toMap() + ("token" to Json.encodeToJsonElement(deviceToken))
        ).let {
            buildJsonObject {
                put("device", it)
            }
        }

        event.context = event.context mergeWithHigherPriorityTo updatedDevice

        return event
    }
}

/**
 * Merges the current JSON object with another JSON object, giving higher priority to the other JSON object.
 *
 * @param other The JSON object to merge with the current JSON object.
 */
private infix fun JsonObject.mergeWithHigherPriorityTo(other: JsonObject): JsonObject {
    return JsonObject(this.toMap() + other.toMap())
}
