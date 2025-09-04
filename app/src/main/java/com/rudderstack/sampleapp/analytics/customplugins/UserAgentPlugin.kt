package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val USER_AGENT_KEY = "userAgent"

/**
 * A plugin that adds User Agent information to the event payload.
 *
 * Add this plugin just after the SDK initialization to include user agent in the event payload.
 *
 * Add the plugin like this:
 * ```
 * analytics.add(UserAgentPlugin())
 * ```
 *
 * This will add the system user agent to the `event.context` payload of each event.
 *
 * @param userAgentProvider A function that provides the user agent string. Defaults to System.getProperty("http.agent").
 */
class UserAgentPlugin(
    private val userAgentProvider: () -> String? = { System.getProperty("http.agent") }
) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    private val userAgent: String? by lazy {
        userAgentProvider()
    }

    override suspend fun intercept(event: Event): Event {
        addUserAgent(event)
        LoggerAnalytics.verbose("UserAgentPlugin: Adding user agent: $userAgent to the event payload.")
        return event
    }

    private fun addUserAgent(event: Event) {
        userAgent?.let {
            val updatedUserAgent = buildJsonObject {
                put(USER_AGENT_KEY, it)
            }

            event.context = event.context mergeWithHigherPriorityTo updatedUserAgent
        }
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
