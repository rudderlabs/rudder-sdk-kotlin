package com.rudderstack.integration.kotlin.braze

import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceParamNames
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.util.Locale

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
private const val PRODUCTS_KEY = ECommerceParamNames.PRODUCTS
private const val METADATA_KEY = "metadata"
private const val ACTION_KEY = "action"

/** On Android the Braze `source` field is always `android` (envelope derivation is skipped). */
private const val ANDROID_SOURCE = "android"

/**
 * The type Braze expects for a recommended-event field. Resolved values are coerced to this type where
 * possible; a value that cannot be coerced is sent verbatim and surfaced via a warning.
 */
internal enum class BrazeFieldType {
    STRING,
    INTEGER,
    FLOAT,
    STRING_ARRAY,
    ARRAY,
}

/**
 * A single field mapping entry, mirroring the cloud `Braze<Event>Config.json` shape.
 *
 * @property destKey The Braze field name to write.
 * @property sourceKeys Ordered fallback chain of RudderStack property names; the first resolved value wins.
 * @property brazeRequired When true, a missing value contributes to the validation warning.
 * @property expectedType The type Braze expects for this field; the value is coerced to it where possible,
 * otherwise sent verbatim with a warning.
 */
internal data class FieldMapping(
    val destKey: String,
    val sourceKeys: List<String>,
    val brazeRequired: Boolean,
    val expectedType: BrazeFieldType = BrazeFieldType.STRING,
)

/** Result of resolving an RS event name to a Braze recommended event. */
internal data class EcommerceMapping(
    val brazeEvent: String,
    val action: String? = null,
)

private fun mapping(
    destKey: String,
    vararg sourceKeys: String,
    brazeRequired: Boolean,
    type: BrazeFieldType = BrazeFieldType.STRING,
) = FieldMapping(destKey, sourceKeys.toList(), brazeRequired, type)

// Source keys that are not part of the RudderStack ecommerce spec (`ECommerceParamNames`) and are
// therefore mirrored verbatim from the Braze recommended-event schema.
private const val SKU = "sku"
private const val NAME = "name"
private const val VARIANT = "variant"
private const val VALUE = "value"
private const val IMAGE_URL = "image_url"
private const val URL = "url"
private const val TYPE = "type"
private const val SUBTOTAL_VALUE = "subtotal_value"
private const val TOTAL_DISCOUNTS = "total_discounts"
private const val DISCOUNTS = "discounts"
private const val CANCEL_REASON = "cancel_reason"
private const val TAX = "tax"
private const val SHIPPING = "shipping"

private val PRODUCT_VIEWED_MAPPING = listOf(
    mapping("product_id", ECommerceParamNames.PRODUCT_ID, SKU, brazeRequired = true),
    mapping("product_name", NAME, brazeRequired = true),
    mapping("variant_id", VARIANT, SKU, ECommerceParamNames.PRODUCT_ID, brazeRequired = true),
    mapping("price", ECommerceParamNames.PRICE, brazeRequired = true, type = BrazeFieldType.FLOAT),
    mapping("currency", ECommerceParamNames.CURRENCY, brazeRequired = true),
    mapping("image_url", IMAGE_URL, brazeRequired = false),
    mapping("product_url", URL, brazeRequired = false),
    mapping("type", TYPE, brazeRequired = false, type = BrazeFieldType.STRING_ARRAY),
)

private val CART_UPDATED_MAPPING = listOf(
    mapping("cart_id", ECommerceParamNames.CART_ID, brazeRequired = true),
    mapping("total_value", ECommerceParamNames.TOTAL, VALUE, brazeRequired = false, type = BrazeFieldType.FLOAT),
    mapping("subtotal_value", SUBTOTAL_VALUE, brazeRequired = false, type = BrazeFieldType.FLOAT),
    mapping("tax", TAX, brazeRequired = false, type = BrazeFieldType.FLOAT),
    mapping("shipping", SHIPPING, brazeRequired = false, type = BrazeFieldType.FLOAT),
    mapping("currency", ECommerceParamNames.CURRENCY, brazeRequired = true),
)

private val CHECKOUT_STARTED_MAPPING = listOf(
    mapping("checkout_id", ECommerceParamNames.CHECKOUT_ID, ECommerceParamNames.ORDER_ID, brazeRequired = true),
    mapping("cart_id", ECommerceParamNames.CART_ID, brazeRequired = false),
    mapping(
        "total_value",
        ECommerceParamNames.TOTAL,
        ECommerceParamNames.REVENUE,
        VALUE,
        brazeRequired = true,
        type = BrazeFieldType.FLOAT,
    ),
    mapping("subtotal_value", SUBTOTAL_VALUE, brazeRequired = false, type = BrazeFieldType.FLOAT),
    mapping("tax", TAX, brazeRequired = false, type = BrazeFieldType.FLOAT),
    mapping("shipping", SHIPPING, brazeRequired = false, type = BrazeFieldType.FLOAT),
    mapping("currency", ECommerceParamNames.CURRENCY, brazeRequired = true),
)

private val ORDER_PLACED_MAPPING = listOf(
    mapping("order_id", ECommerceParamNames.ORDER_ID, brazeRequired = true),
    mapping(
        "total_value",
        ECommerceParamNames.TOTAL,
        ECommerceParamNames.REVENUE,
        VALUE,
        brazeRequired = true,
        type = BrazeFieldType.FLOAT,
    ),
    mapping("currency", ECommerceParamNames.CURRENCY, brazeRequired = true),
    mapping("cart_id", ECommerceParamNames.CART_ID, brazeRequired = false),
    mapping("tax", TAX, brazeRequired = false, type = BrazeFieldType.FLOAT),
    mapping("shipping", SHIPPING, brazeRequired = false, type = BrazeFieldType.FLOAT),
    mapping(
        "total_discounts",
        ECommerceParamNames.DISCOUNT,
        TOTAL_DISCOUNTS,
        brazeRequired = false,
        type = BrazeFieldType.FLOAT,
    ),
    mapping("subtotal_value", SUBTOTAL_VALUE, brazeRequired = false, type = BrazeFieldType.FLOAT),
    mapping("discounts", DISCOUNTS, brazeRequired = false, type = BrazeFieldType.ARRAY),
)

private val ORDER_REFUNDED_MAPPING = listOf(
    mapping("order_id", ECommerceParamNames.ORDER_ID, brazeRequired = true),
    mapping(
        "total_value",
        ECommerceParamNames.TOTAL,
        ECommerceParamNames.REVENUE,
        VALUE,
        brazeRequired = true,
        type = BrazeFieldType.FLOAT,
    ),
    mapping("currency", ECommerceParamNames.CURRENCY, brazeRequired = true),
    mapping(
        "total_discounts",
        ECommerceParamNames.DISCOUNT,
        TOTAL_DISCOUNTS,
        brazeRequired = false,
        type = BrazeFieldType.FLOAT,
    ),
    mapping("discounts", DISCOUNTS, brazeRequired = false, type = BrazeFieldType.ARRAY),
)

private val ORDER_CANCELLED_MAPPING = listOf(
    mapping("order_id", ECommerceParamNames.ORDER_ID, brazeRequired = true),
    mapping(
        "total_value",
        ECommerceParamNames.TOTAL,
        ECommerceParamNames.REVENUE,
        VALUE,
        brazeRequired = true,
        type = BrazeFieldType.FLOAT,
    ),
    mapping("currency", ECommerceParamNames.CURRENCY, brazeRequired = true),
    mapping("cancel_reason", CANCEL_REASON, ECommerceParamNames.REASON, brazeRequired = true),
    mapping("tax", TAX, brazeRequired = false, type = BrazeFieldType.FLOAT),
    mapping("shipping", SHIPPING, brazeRequired = false, type = BrazeFieldType.FLOAT),
    mapping(
        "total_discounts",
        ECommerceParamNames.DISCOUNT,
        TOTAL_DISCOUNTS,
        brazeRequired = false,
        type = BrazeFieldType.FLOAT,
    ),
    mapping("subtotal_value", SUBTOTAL_VALUE, brazeRequired = false, type = BrazeFieldType.FLOAT),
    mapping("discounts", DISCOUNTS, brazeRequired = false, type = BrazeFieldType.ARRAY),
)

private val ECOMMERCE_PRODUCT_MAPPING = listOf(
    mapping("product_id", ECommerceParamNames.PRODUCT_ID, SKU, brazeRequired = true),
    mapping("product_name", NAME, brazeRequired = true),
    mapping("variant_id", VARIANT, SKU, ECommerceParamNames.PRODUCT_ID, brazeRequired = true),
    mapping("quantity", ECommerceParamNames.QUANTITY, brazeRequired = true, type = BrazeFieldType.INTEGER),
    mapping("price", ECommerceParamNames.PRICE, brazeRequired = true, type = BrazeFieldType.FLOAT),
    mapping("image_url", IMAGE_URL, brazeRequired = false),
    mapping("product_url", URL, brazeRequired = false),
)

private val EVENT_MAPPING_BY_BRAZE_EVENT: Map<String, List<FieldMapping>> = mapOf(
    BrazeEcommerceEvents.PRODUCT_VIEWED to PRODUCT_VIEWED_MAPPING,
    BrazeEcommerceEvents.CART_UPDATED to CART_UPDATED_MAPPING,
    BrazeEcommerceEvents.CHECKOUT_STARTED to CHECKOUT_STARTED_MAPPING,
    BrazeEcommerceEvents.ORDER_PLACED to ORDER_PLACED_MAPPING,
    BrazeEcommerceEvents.ORDER_REFUNDED to ORDER_REFUNDED_MAPPING,
    BrazeEcommerceEvents.ORDER_CANCELLED to ORDER_CANCELLED_MAPPING,
)

// Normalized RS event name → Braze recommended event. `Cart Viewed` and `Cart Updated` are
// intentionally absent — they fall through to the legacy custom-event path.
private val EVENT_NAME_TO_BRAZE: Map<String, EcommerceMapping> = mapOf(
    ECommerceEvents.PRODUCT_VIEWED to EcommerceMapping(BrazeEcommerceEvents.PRODUCT_VIEWED),
    ECommerceEvents.PRODUCT_ADDED to EcommerceMapping(BrazeEcommerceEvents.CART_UPDATED, CartUpdatedAction.ADD),
    ECommerceEvents.PRODUCT_REMOVED to EcommerceMapping(BrazeEcommerceEvents.CART_UPDATED, CartUpdatedAction.REMOVE),
    ECommerceEvents.CHECKOUT_STARTED to EcommerceMapping(BrazeEcommerceEvents.CHECKOUT_STARTED),
    ECommerceEvents.ORDER_COMPLETED to EcommerceMapping(BrazeEcommerceEvents.ORDER_PLACED),
    ECommerceEvents.ORDER_REFUNDED to EcommerceMapping(BrazeEcommerceEvents.ORDER_REFUNDED),
    ECommerceEvents.ORDER_CANCELLED to EcommerceMapping(BrazeEcommerceEvents.ORDER_CANCELLED),
).mapKeys { normalizeEventName(it.key) }

/** Normalizes an RS event name for case-insensitive lookup using a locale-independent transform. */
private fun normalizeEventName(eventName: String): String = eventName.trim().lowercase(Locale.ROOT)

/**
 * Resolves the Braze recommended event for a given RudderStack event name.
 * Matching is case-insensitive on the trimmed event name.
 *
 * @return The [EcommerceMapping] or `null` when the event is not mapped (caller falls back to the legacy path).
 */
internal fun getEcommerceMapping(eventName: String?): EcommerceMapping? =
    eventName?.let { EVENT_NAME_TO_BRAZE[normalizeEventName(it)] }

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
    // Only an array whose elements are all objects is treated as an explicit products[]. A malformed
    // array (containing non-object elements) is left unconsumed so it flows through to metadata.
    val explicitProducts = properties.explicitProductsArray()
    val hasExplicitProductsArray = explicitProducts != null

    // Step 1+2: event-level field mapping.
    val payload: MutableMap<String, JsonElement> = properties.resolveMapping(eventMapping).toMutableMap()

    // Step 3: products[] (skipped for product_viewed — flat, single-product event).
    val products: List<JsonObject>? = productMapping?.let {
        buildProductsArray(properties, brazeEvent, it, explicitProducts)
    }
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

    // Step 8: warn on any resolved field whose value type still doesn't match Braze's schema after coercion.
    logTypeMismatchedFields(brazeEvent, eventMapping, productMapping, payload, products, logger)

    return JsonObject(payload)
}

/**
 * Builds the `products[]` array.
 * - `cart_updated` WITHOUT an explicit `products[]`: read top-level product fields directly from [properties]
 *   into a single-element list (no per-product metadata — unmapped keys flow through event-level metadata).
 * - all other cases: map each item in [explicitProducts] and route unmapped per-product keys to `metadata`.
 *
 * @param explicitProducts The validated array-of-objects `products[]`, or `null` when absent/malformed.
 */
private fun buildProductsArray(
    properties: JsonObject,
    brazeEvent: String,
    productMapping: List<FieldMapping>,
    explicitProducts: JsonArray?,
): List<JsonObject> {
    if (brazeEvent == BrazeEcommerceEvents.CART_UPDATED && explicitProducts == null) {
        val product = properties.resolveMapping(productMapping)
        return if (product.isNotEmpty()) listOf(JsonObject(product)) else emptyList()
    }

    val consumedKeys = productMapping.consumedSourceKeys()
    return explicitProducts.orEmpty()
        .filterIsInstance<JsonObject>()
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
 * Returns the `products` value as an array only when it is present and every element is a [JsonObject].
 * A non-array or mixed/malformed array returns `null` so the original value can flow through to metadata.
 */
private fun JsonObject.explicitProductsArray(): JsonArray? =
    (this[PRODUCTS_KEY] as? JsonArray)?.takeIf { array -> array.all { it is JsonObject } }

/**
 * Resolves a field mapping against this object, keeping only entries whose first non-empty source key resolves.
 */
private fun JsonObject.resolveMapping(mapping: List<FieldMapping>): Map<String, JsonElement> {
    val result = LinkedHashMap<String, JsonElement>()
    mapping.forEach { entry ->
        entry.sourceKeys
            .firstNotNullOfOrNull { key -> this[key]?.takeIf { it.isResolved() } }
            ?.let { result[entry.destKey] = it.coerceToType(entry.expectedType) }
    }
    return result
}

/**
 * Coerces a primitive value to the [type] Braze expects, where possible (e.g. numeric string → number,
 * integer → float, number/boolean → string). Returns the value unchanged when it cannot be coerced
 * (the residual mismatch is then surfaced by [logTypeMismatchedFields]); arrays/objects are never coerced.
 */
private fun JsonElement.coerceToType(type: BrazeFieldType): JsonElement {
    if (this !is JsonPrimitive) return this
    return when (type) {
        BrazeFieldType.STRING -> if (isString) this else JsonPrimitive(content)
        BrazeFieldType.FLOAT -> doubleOrNull?.let { JsonPrimitive(it) } ?: this
        BrazeFieldType.INTEGER -> longOrNull?.let { JsonPrimitive(it) } ?: this
        BrazeFieldType.STRING_ARRAY, BrazeFieldType.ARRAY -> this
    }
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
 * Logs a single warning for any resolved field whose value still does not match the type Braze expects after
 * coercion (event-level or per-product). The (un-coercible) value is sent verbatim; this surfaces it for visibility.
 */
private fun logTypeMismatchedFields(
    brazeEvent: String,
    eventMapping: List<FieldMapping>,
    productMapping: List<FieldMapping>?,
    payload: Map<String, JsonElement>,
    products: List<JsonObject>?,
    logger: Logger,
) {
    val mismatched = mutableListOf<String>()

    eventMapping.forEach { entry ->
        payload[entry.destKey]?.takeIf { !it.matchesType(entry.expectedType) }
            ?.let { mismatched.add("${entry.destKey} (expected ${entry.expectedType})") }
    }

    if (productMapping != null && !products.isNullOrEmpty()) {
        productMapping.forEach { entry ->
            val hasMismatch = products.any { product ->
                product[entry.destKey]?.let { !it.matchesType(entry.expectedType) } == true
            }
            if (hasMismatch) {
                mismatched.add("products[].${entry.destKey} (expected ${entry.expectedType})")
            }
        }
    }

    if (mismatched.isNotEmpty()) {
        logger.warn(
            "BrazeIntegration: '$brazeEvent' has type-mismatched field(s) (sent as-is): ${mismatched.distinct()}."
        )
    }
}

/**
 * Returns whether this value matches the [type] Braze expects. `0`/`false` are valid for their respective types;
 * a numeric written as a string (e.g. `"29.99"`) does not match a numeric type.
 */
private fun JsonElement.matchesType(type: BrazeFieldType): Boolean = when (type) {
    BrazeFieldType.STRING -> this is JsonPrimitive && isString
    BrazeFieldType.INTEGER -> this is JsonPrimitive && !isString && longOrNull != null
    BrazeFieldType.FLOAT -> this is JsonPrimitive && !isString && doubleOrNull != null
    BrazeFieldType.STRING_ARRAY -> this is JsonArray && all { it is JsonPrimitive && it.isString }
    BrazeFieldType.ARRAY -> this is JsonArray
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
