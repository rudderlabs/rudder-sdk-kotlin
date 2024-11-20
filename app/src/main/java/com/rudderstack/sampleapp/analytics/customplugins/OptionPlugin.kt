package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.logger.LoggerAnalytics
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.models.RudderOption
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import kotlinx.serialization.json.JsonObject

/**
 * A plugin that adds custom context and integrations to each message. Note: External IDs should not be updated here.
 *
 * Add this plugin during SDK initialization to apply custom context and integrations to all messages.
 * It overrides any individual context and integrations set within a message with the provided custom values.
 *
 * @param option The custom option to be added to each message.
 */
class OptionPlugin (
    private val option: RudderOption = RudderOption()
): Plugin {

    override val pluginType = Plugin.PluginType.OnProcess

    override lateinit var analytics: Analytics

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        this.analytics = analytics
    }

    override suspend fun execute(message: Message): Message {
        addCustomContext(message)
        addIntegrations(message)
        // NOTE: Don't update the externalIds, as it should be updated only through the Identify event.
        LoggerAnalytics.verbose("OptionPlugin: Added custom context and integrations to the message.")
        return message
    }

    private fun addCustomContext(message: Message) {
        message.context = message.context mergeWithHigherPriorityTo option.customContext
    }

    private fun addIntegrations(message: Message) {
        message.integrations = message.integrations mergeWithHigherPriorityTo option.integrations
    }
}

/**
 * Merges the current JSON object with another JSON object, giving higher priority to the other JSON object.
 *
 * @param other The JSON object to merge with the current JSON object.
 */
infix fun JsonObject.mergeWithHigherPriorityTo(other: JsonObject): JsonObject {
    return JsonObject(this.toMap() + other.toMap())
}

