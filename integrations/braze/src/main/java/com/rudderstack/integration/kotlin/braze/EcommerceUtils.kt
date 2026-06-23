package com.rudderstack.integration.kotlin.braze

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Braze recommended ecommerce event names.
 * See https://www.braze.com/docs/user_guide/data/activation/events/recommended_events
 */
internal object BrazeEcommerceEvents {

    const val PRODUCT_VIEWED = "ecommerce.product_viewed"
    const val CART_UPDATED = "ecommerce.cart_updated"
    const val CHECKOUT_STARTED = "ecommerce.checkout_started"
    const val ORDER_PLACED = "ecommerce.order_placed"
    const val ORDER_REFUNDED = "ecommerce.order_refunded"
    const val ORDER_CANCELLED = "ecommerce.order_cancelled"
}

/** Action carried by `ecommerce.cart_updated` events. */
internal object CartUpdatedAction {

    const val ADD = "add"
    const val REMOVE = "remove"
}

private const val SOURCE_KEY = "source"
private const val PRODUCTS_KEY = "products"
private const val METADATA_KEY = "metadata"
private const val ACTION_KEY = "action"

/** On Android the Braze `source` field is always `android` (envelope derivation is skipped). */
private const val ANDROID_SOURCE = "android"

/**
 * A single field mapping entry, mirroring the cloud `Braze<Event>Config.json` shape.
 *
 * @property destKey The Braze field name to write.
 * @property sourceKeys Ordered fallback chain of RudderStack property names; the first resolved value wins.
 * @property brazeRequired When true, a missing value contributes to the validation warning.
 */
internal data class FieldMapping(
    val destKey: String,
    val sourceKeys: List<String>,
    val brazeRequired: Boolean,
)

/** Result of resolving an RS event name to a Braze recommended event. */
internal data class EcommerceMapping(
    val brazeEvent: String,
    val action: String? = null,
)

private fun mapping(destKey: String, vararg sourceKeys: String, brazeRequired: Boolean) =
    FieldMapping(destKey, sourceKeys.toList(), brazeRequired)

private val PRODUCT_VIEWED_MAPPING = listOf(
    mapping("product_id", "product_id", "sku", brazeRequired = true),
    mapping("product_name", "name", brazeRequired = true),
    mapping("variant_id", "variant", "sku", "product_id", brazeRequired = true),
    mapping("price", "price", brazeRequired = true),
    mapping("currency", "currency", brazeRequired = true),
    mapping("image_url", "image_url", brazeRequired = false),
    mapping("product_url", "url", brazeRequired = false),
    mapping("type", "type", brazeRequired = false),
)

private val CART_UPDATED_MAPPING = listOf(
    mapping("cart_id", "cart_id", brazeRequired = true),
    mapping("total_value", "total", "value", brazeRequired = false),
    mapping("subtotal_value", "subtotal_value", brazeRequired = false),
    mapping("tax", "tax", brazeRequired = false),
    mapping("shipping", "shipping", brazeRequired = false),
    mapping("currency", "currency", brazeRequired = true),
)

private val CHECKOUT_STARTED_MAPPING = listOf(
    mapping("checkout_id", "checkout_id", "order_id", brazeRequired = true),
    mapping("cart_id", "cart_id", brazeRequired = false),
    mapping("total_value", "total", "revenue", "value", brazeRequired = true),
    mapping("subtotal_value", "subtotal_value", brazeRequired = false),
    mapping("tax", "tax", brazeRequired = false),
    mapping("shipping", "shipping", brazeRequired = false),
    mapping("currency", "currency", brazeRequired = true),
)

private val ORDER_PLACED_MAPPING = listOf(
    mapping("order_id", "order_id", brazeRequired = true),
    mapping("total_value", "total", "revenue", "value", brazeRequired = true),
    mapping("currency", "currency", brazeRequired = true),
    mapping("cart_id", "cart_id", brazeRequired = false),
    mapping("tax", "tax", brazeRequired = false),
    mapping("shipping", "shipping", brazeRequired = false),
    mapping("total_discounts", "discount", "total_discounts", brazeRequired = false),
    mapping("subtotal_value", "subtotal_value", brazeRequired = false),
    mapping("discounts", "discounts", brazeRequired = false),
)

private val ORDER_REFUNDED_MAPPING = listOf(
    mapping("order_id", "order_id", brazeRequired = true),
    mapping("total_value", "total", "revenue", "value", brazeRequired = true),
    mapping("currency", "currency", brazeRequired = true),
    mapping("total_discounts", "discount", "total_discounts", brazeRequired = false),
    mapping("discounts", "discounts", brazeRequired = false),
)

private val ORDER_CANCELLED_MAPPING = listOf(
    mapping("order_id", "order_id", brazeRequired = true),
    mapping("total_value", "total", "revenue", "value", brazeRequired = true),
    mapping("currency", "currency", brazeRequired = true),
    mapping("cancel_reason", "cancel_reason", "reason", brazeRequired = true),
    mapping("tax", "tax", brazeRequired = false),
    mapping("shipping", "shipping", brazeRequired = false),
    mapping("total_discounts", "discount", "total_discounts", brazeRequired = false),
    mapping("subtotal_value", "subtotal_value", brazeRequired = false),
    mapping("discounts", "discounts", brazeRequired = false),
)

private val ECOMMERCE_PRODUCT_MAPPING = listOf(
    mapping("product_id", "product_id", "sku", brazeRequired = true),
    mapping("product_name", "name", brazeRequired = true),
    mapping("variant_id", "variant", "sku", "product_id", brazeRequired = true),
    mapping("quantity", "quantity", brazeRequired = true),
    mapping("price", "price", brazeRequired = true),
    mapping("image_url", "image_url", brazeRequired = false),
    mapping("product_url", "url", brazeRequired = false),
)

private val EVENT_MAPPING_BY_BRAZE_EVENT: Map<String, List<FieldMapping>> = mapOf(
    BrazeEcommerceEvents.PRODUCT_VIEWED to PRODUCT_VIEWED_MAPPING,
    BrazeEcommerceEvents.CART_UPDATED to CART_UPDATED_MAPPING,
    BrazeEcommerceEvents.CHECKOUT_STARTED to CHECKOUT_STARTED_MAPPING,
    BrazeEcommerceEvents.ORDER_PLACED to ORDER_PLACED_MAPPING,
    BrazeEcommerceEvents.ORDER_REFUNDED to ORDER_REFUNDED_MAPPING,
    BrazeEcommerceEvents.ORDER_CANCELLED to ORDER_CANCELLED_MAPPING,
)

// Case-insensitive RS event name (trimmed, lowercased) → Braze recommended event.
// `Cart Viewed` and `Cart Updated` are intentionally absent — they fall through to the
// legacy custom-event path.
private val EVENT_NAME_TO_BRAZE: Map<String, EcommerceMapping> = mapOf(
    "product viewed" to EcommerceMapping(BrazeEcommerceEvents.PRODUCT_VIEWED),
    "product added" to EcommerceMapping(BrazeEcommerceEvents.CART_UPDATED, CartUpdatedAction.ADD),
    "product removed" to EcommerceMapping(BrazeEcommerceEvents.CART_UPDATED, CartUpdatedAction.REMOVE),
    "checkout started" to EcommerceMapping(BrazeEcommerceEvents.CHECKOUT_STARTED),
    "order completed" to EcommerceMapping(BrazeEcommerceEvents.ORDER_PLACED),
    "order refunded" to EcommerceMapping(BrazeEcommerceEvents.ORDER_REFUNDED),
    "order cancelled" to EcommerceMapping(BrazeEcommerceEvents.ORDER_CANCELLED),
)

/**
 * Resolves the Braze recommended event for a given RudderStack event name.
 * Matching is case-insensitive on the trimmed event name.
 *
 * @return The [EcommerceMapping] or `null` when the event is not mapped (caller falls back to the legacy path).
 */
internal fun getEcommerceMapping(eventName: String?): EcommerceMapping? =
    eventName?.trim()?.lowercase()?.let { EVENT_NAME_TO_BRAZE[it] }

/**
 * Builds the `properties` object for a Braze recommended ecommerce event.
 *
 * Never throws on data shape: `0`/`false` are valid values; only `null`/empty-string/empty-array/empty-object
 * count as missing. A single warning is logged when any Braze-required field is unresolved.
 *
 * @param properties The track event properties.
 * @param brazeEvent The resolved Braze recommended event name.
 * @param action The `cart_updated` action, or `null` for other events.
 * @param logger Used to surface the validation warning.
 * @return The fully assembled Braze event properties.
 */
internal fun buildEcommerceEventProperties(
    properties: JsonObject,
    brazeEvent: String,
    action: String?,
    logger: Logger,
): JsonObject {
    val eventMapping = EVENT_MAPPING_BY_BRAZE_EVENT[brazeEvent].orEmpty()
    val productMapping = if (brazeEvent == BrazeEcommerceEvents.PRODUCT_VIEWED) null else ECOMMERCE_PRODUCT_MAPPING
    val hasExplicitProductsArray = properties[PRODUCTS_KEY] is JsonArray

    // Step 1+2: event-level field mapping.
    val payload: MutableMap<String, JsonElement> = properties.resolveMapping(eventMapping).toMutableMap()

    // Step 3: products[] (skipped for product_viewed — flat, single-product event).
    val products: List<JsonObject>? = productMapping?.let { buildProductsArray(properties, brazeEvent, it) }
    if (products != null && products.isNotEmpty()) {
        payload[PRODUCTS_KEY] = JsonArray(products)
    }

    // Step 4+5: source + action.
    payload[SOURCE_KEY] = JsonPrimitive(ANDROID_SOURCE)
    action?.let { payload[ACTION_KEY] = JsonPrimitive(it) }

    // Step 6: route unmapped event-level keys to metadata.
    val consumedEventKeys = consumedTopLevelKeysForEvent(
        brazeEvent = brazeEvent,
        eventMapping = eventMapping,
        productMapping = productMapping,
        hasExplicitProductsArray = hasExplicitProductsArray,
    ).toMutableSet()
    action?.let { consumedEventKeys.add(ACTION_KEY) }
    val metadata = properties.pickUnmappedKeys(consumedEventKeys)
    if (metadata.isNotEmpty()) {
        payload[METADATA_KEY] = JsonObject(metadata)
    }

    // Step 7: single validation warning for any missing Braze-required field.
    logMissingRequiredFields(brazeEvent, eventMapping, productMapping, payload, products, logger)

    return JsonObject(payload)
}

/**
 * Builds the `products[]` array.
 * - `cart_updated` WITHOUT an explicit `products[]`: read top-level product fields directly from [properties]
 *   into a single-element list (no per-product metadata — unmapped keys flow through event-level metadata).
 * - all other cases: map each item in `properties.products` and route unmapped per-product keys to `metadata`.
 */
private fun buildProductsArray(
    properties: JsonObject,
    brazeEvent: String,
    productMapping: List<FieldMapping>,
): List<JsonObject> {
    val rawProducts = properties[PRODUCTS_KEY] as? JsonArray

    if (brazeEvent == BrazeEcommerceEvents.CART_UPDATED && rawProducts == null) {
        val product = properties.resolveMapping(productMapping)
        return if (product.isNotEmpty()) listOf(JsonObject(product)) else emptyList()
    }

    val consumedKeys = productMapping.consumedSourceKeys()
    return rawProducts.orEmpty()
        .mapNotNull { it as? JsonObject }
        .map { item ->
            val product = item.resolveMapping(productMapping).toMutableMap()
            val productMetadata = item.pickUnmappedKeys(consumedKeys)
            if (productMetadata.isNotEmpty()) {
                product[METADATA_KEY] = JsonObject(productMetadata)
            }
            JsonObject(product)
        }
        .filter { it.isNotEmpty() }
}

/**
 * Resolves a field mapping against this object, keeping only entries whose first non-empty source key resolves.
 */
private fun JsonObject.resolveMapping(mapping: List<FieldMapping>): Map<String, JsonElement> {
    val result = LinkedHashMap<String, JsonElement>()
    mapping.forEach { entry ->
        entry.sourceKeys
            .firstNotNullOfOrNull { key -> this[key]?.takeIf { it.isResolved() } }
            ?.let { result[entry.destKey] = it }
    }
    return result
}

/**
 * Returns the subset of this object's keys not present in [consumed], dropping unresolved values.
 */
private fun JsonObject.pickUnmappedKeys(consumed: Set<String>): Map<String, JsonElement> {
    val result = LinkedHashMap<String, JsonElement>()
    forEach { (key, value) ->
        if (key !in consumed && value.isResolved()) {
            result[key] = value
        }
    }
    return result
}

/**
 * Computes the message-property keys consumed by the event-level mapping so they don't duplicate into metadata.
 */
private fun consumedTopLevelKeysForEvent(
    brazeEvent: String,
    eventMapping: List<FieldMapping>,
    productMapping: List<FieldMapping>?,
    hasExplicitProductsArray: Boolean,
): Set<String> {
    val consumed = mutableSetOf(SOURCE_KEY)
    consumed.addAll(eventMapping.consumedSourceKeys())
    // Only consume `products` when it is an actual array we build from. A malformed (non-array) `products`
    // value is left unconsumed so it flows through to metadata instead of being silently dropped.
    if (productMapping != null && hasExplicitProductsArray) {
        consumed.add(PRODUCTS_KEY)
    }
    // cart_updated folds top-level product fields into products[0] only when no explicit products[] is provided.
    if (brazeEvent == BrazeEcommerceEvents.CART_UPDATED && productMapping != null && !hasExplicitProductsArray) {
        consumed.addAll(productMapping.consumedSourceKeys())
    }
    return consumed
}

/** All source keys referenced by a mapping. */
private fun List<FieldMapping>.consumedSourceKeys(): Set<String> = flatMapTo(mutableSetOf()) { it.sourceKeys }

/**
 * Logs a single warning when the payload is missing any Braze-required field (event-level or per-product),
 * including an empty `products[]` on a product-bearing event.
 */
private fun logMissingRequiredFields(
    brazeEvent: String,
    eventMapping: List<FieldMapping>,
    productMapping: List<FieldMapping>?,
    payload: Map<String, JsonElement>,
    products: List<JsonObject>?,
    logger: Logger,
) {
    val missing = mutableListOf<String>()

    eventMapping.filter { it.brazeRequired && payload[it.destKey] == null }
        .forEach { missing.add(it.destKey) }

    if (productMapping != null) {
        if (products.isNullOrEmpty()) {
            missing.add(PRODUCTS_KEY)
        } else {
            val missingProductFields = productMapping
                .filter { entry -> entry.brazeRequired && products.any { it[entry.destKey] == null } }
                .map { "products[].${it.destKey}" }
            missing.addAll(missingProductFields)
        }
    }

    if (missing.isNotEmpty()) {
        logger.warn(
            "BrazeIntegration: '$brazeEvent' is missing Braze-required field(s): " +
                "${missing.distinct()}. Sending the event anyway."
        )
    }
}

/**
 * A value is "resolved" iff it is not null/empty: `null`, [JsonNull], empty string, empty array and empty object
 * count as missing; `0`, `false`, and other primitives are valid.
 */
private fun JsonElement.isResolved(): Boolean = when (this) {
    is JsonNull -> false
    is JsonPrimitive -> if (isString) content.isNotEmpty() else true
    is JsonArray -> isNotEmpty()
    is JsonObject -> isNotEmpty()
}
