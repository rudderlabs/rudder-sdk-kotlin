package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.ExternalId
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray

/**
 * A plugin that adds custom context, integrations and externalIds to each message.
 *
 * Add this plugin just after the SDK initialization to apply custom context, integrations and externalIds to all messages.
 *
 * **NOTE: It overrides any individual context and integrations set within a message with the provided custom values.**
 *
 * Add the plugin like this:
 * ```
 * analytics.add(
 *     GlobalRudderOptionPlugin(
 *         option = RudderOption(
 *             customContext = buildJsonObject {
 *                 put("key", "value")
 *             },
 *             integrations = buildJsonObject {
 *                 put("CleverTap", true)
 *             },
 *             externalIds = listOf(
 *                 ExternalId(type = "globalExternalId", id = "someValue"),
 *             )
 *         )
 *     )
 * )
 * ```
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

    override suspend fun intercept(event: Event): Event {
        addCustomContext(event)
        addIntegrations(event)
        addExternalIds(event)
        LoggerAnalytics.verbose("OptionPlugin: Added custom context, integrations and externalIds to the message.")
        return event
    }

    private fun addCustomContext(event: Event) {
        event.context = event.context mergeWithHigherPriorityTo option.customContext
    }

    private fun addIntegrations(event: Event) {
        event.integrations = event.integrations mergeWithHigherPriorityTo option.integrations
    }

    private fun addExternalIds(event: Event) {
        val externalIds = mergeExternalIds(event)
        event.context = event.context mergeWithHigherPriorityTo externalIds.toJsonObject()
    }

    private fun mergeExternalIds(event: Event): List<ExternalId> {
        val currentExternalIds: List<ExternalId> = event.context["externalId"]?.jsonArray?.map {
            Json.decodeFromJsonElement(it)
        } ?: emptyList()

        val currentExternalIdMap = currentExternalIds.associateBy { it.type }.toMutableMap()

        option.externalIds.forEach { newExternalId ->
            currentExternalIdMap[newExternalId.type] = newExternalId
        }

        return currentExternalIdMap.values.toList()
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

/**
 * Converts a list of `ExternalIds` to a JsonObject.
 *
 * If the list is empty, the method returns an empty JSON object.
 * Else the method returns a JSON object with the following structure:
 *
 * ```
 * "externalId": [
 *     {
 *         "id": "<some-value>",
 *         "type": "brazeExternalId"
 *     },
 *     {
 *         "id": "<some-value>",
 *         "type": "ga4"
 *     }
 * ],
 * ```
 */
private fun List<ExternalId>.toJsonObject(): JsonObject {
    if (this.isEmpty()) return emptyJsonObject
    val externalIds = Json.encodeToJsonElement(this) as JsonArray
    return buildJsonObject {
        put("externalId", externalIds)
    }
}
