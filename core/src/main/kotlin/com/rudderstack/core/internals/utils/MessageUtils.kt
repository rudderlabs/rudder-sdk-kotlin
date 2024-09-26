package com.rudderstack.core.internals.utils

import com.rudderstack.core.internals.models.GroupEvent
import com.rudderstack.core.internals.models.Message
import com.rudderstack.core.internals.models.RudderTraits
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val NAME = "name"
private const val CATEGORY = "category"

internal fun addNameAndCategoryToProperties(name: String, category: String, properties: JsonObject): JsonObject {
    val nameAndCategoryProperties: JsonObject =
        buildJsonObject {
            if (name.isNotEmpty()) {
                put(NAME, name)
            }
            if (category.isNotEmpty()) {
                put(CATEGORY, category)
            }
        }

    return properties mergeWithHigherPriorityTo nameAndCategoryProperties
}

internal fun Message.addAnonymousIdToTraits() {
    when (this) {
        is GroupEvent -> {
            val updatedTraits = getTraitsWithAnonymousId(anonymousId = anonymousId, traits = this.traits)
            this.traits = updatedTraits
        }

        else -> {
            // NO-OP
        }
    }
}

private fun getTraitsWithAnonymousId(anonymousId: String, traits: RudderTraits): RudderTraits {
    return if (traits.isNotEmpty()) {
        val traitsWithAnonymousId = buildJsonObject {
            put("anonymousId", anonymousId)
        }
        traits mergeWithHigherPriorityTo traitsWithAnonymousId
    } else {
        traits
    }
}
