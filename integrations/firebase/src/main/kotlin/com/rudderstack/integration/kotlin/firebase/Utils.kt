package com.rudderstack.integration.kotlin.firebase

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.rudderstack.sdk.kotlin.android.utils.toBoolean
import com.rudderstack.sdk.kotlin.android.utils.toDouble
import com.rudderstack.sdk.kotlin.android.utils.toInt
import com.rudderstack.sdk.kotlin.android.utils.toLong
import com.rudderstack.sdk.kotlin.android.utils.toContentString
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceParamNames
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import java.util.Locale

internal val IDENTIFY_RESERVED_KEYWORDS = listOf("age", "gender", "interest")

internal val TRACK_RESERVED_KEYWORDS = listOf(
    "product_id", "name", "category", "quantity", "price", "currency", "value", "revenue", "total",
    "tax", "shipping", "coupon", "cart_id", "payment_method", "query", "list_id", "promotion_id", "creative",
    "affiliation", "share_via", "order_id", ECommerceParamNames.PRODUCTS, FirebaseAnalytics.Param.SCREEN_NAME
)

internal fun getTrimKey(key: String): String {
    return key
        .lowercase(Locale.getDefault())
        .trim()
        .replace(" ", "_")
        .apply {
            if (this.length > 40) {
                this.substring(0, 40)
            }
        }
}

internal fun attachAllCustomProperties(params: Bundle, properties: JsonObject?) {
    if (properties.isNullOrEmpty()) {
        return
    }
    for (key in properties.keys) {
        val firebaseKey: String = getTrimKey(key)
        val value = properties[key]
        if (TRACK_RESERVED_KEYWORDS.contains(firebaseKey) || value?.toString().isNullOrEmpty()) {
            continue
        }

        when {
            checkType(value, String::class.java) -> {
                var stringVal = value?.toContentString() ?: String.empty()
                if (stringVal.length > 100) {
                    stringVal = stringVal.substring(0, 100)
                }
                params.putString(firebaseKey, stringVal)
            }
            checkType(value, Int::class.java) -> params.putInt(firebaseKey, value?.toInt() ?: 0)

            checkType(value, Long::class.java) -> params.putLong(firebaseKey, value?.toLong() ?: 0)

            checkType(value, Double::class.java) -> params.putDouble(firebaseKey, value?.toDouble() ?: 0.0)

            checkType(value, Boolean::class.java) -> params.putBoolean(firebaseKey, value?.toBoolean() ?: false)

            else -> {
                val stringValue = value.toString()
                // if length exceeds 100, don't send the property
                if (stringValue.length <= 100) {
                    params.putString(firebaseKey, stringValue)
                }
            }
        }
    }
}

internal fun checkType(element: JsonElement?, clazz: Class<*>): Boolean {
    return when (clazz) {
        String::class.java -> element is JsonPrimitive && element.isString
        Int::class.java -> element is JsonPrimitive && element.intOrNull != null
        Double::class.java -> element is JsonPrimitive && element.doubleOrNull != null
        Boolean::class.java -> element is JsonPrimitive && element.booleanOrNull != null
        JsonArray::class.java -> element is JsonArray
        JsonObject::class.java -> element is JsonObject
        else -> false
    }
}
