package com.rudderstack.core.internals.utils

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun addNameAndCategoryToProperties(name: String, category: String, properties: JsonObject): JsonObject {
    val nameAndCategoryProperties: JsonObject =
        buildJsonObject {
            if (name.isNotEmpty()) {
                put("name", name)
            }
            if (category.isNotEmpty()) {
                put("category", category)
            }
        }

    return properties mergeWithHigherPriorityTo nameAndCategoryProperties
}
