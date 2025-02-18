package com.rudderstack.integration.kotlin.braze

import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Parses the [JsonObject] to the specified type [T].
 */
@OptIn(InternalRudderApi::class)
internal inline fun <reified T> JsonObject.parse() = LenientJson.decodeFromJsonElement<T>(this)

/**
 * Extension function to parse a JsonObject into StandardProperties.
 *
 * @return StandardProperties object parsed from the JsonObject.
 */
internal fun JsonObject.getStandardProperties(): StandardProperties {
    return this.parse<StandardProperties>()
}

/**
 * Extension function to get the custom properties from the [JsonObject].
 *
 * @param filteredRootKeys The list of keys to be filtered from the root level. Default is `products`.
 * @param filteredProductKeys The list of keys to be filtered from the products. Default is `product_id` and `price`.
 * @return CustomProperties object parsed from the JsonObject.
 */
internal fun JsonObject.getCustomProperties(
    filteredRootKeys: List<String> = listOf("products"),
    filteredProductKeys: List<String> = listOf("product_id", "price"),
): JsonObject {
    val filteredRootProperties = JsonObject(this.filter { it.key !in filteredRootKeys })

    val listOfProducts: JsonArray? = this["products"]?.jsonArray
    val filteredProductProperties: JsonObject =
        listOfProducts?.mapNotNull { it.jsonObject }
            .orEmpty()
            .flatMap { it.entries }
            .filterNot { it.key in filteredProductKeys }
            .associate { it.key to it.value }
            .let(::JsonObject)

    return JsonObject(filteredRootProperties + filteredProductProperties)
}
