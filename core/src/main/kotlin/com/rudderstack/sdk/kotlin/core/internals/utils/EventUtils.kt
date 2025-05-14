package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val NAME = "name"
private const val CATEGORY = "category"
private const val TRAITS = "traits"

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

private val DEFAULT_INTEGRATIONS = buildJsonObject {
    put("All", true)
}

internal fun Event.addRudderOptionFields() {
    this.integrations = DEFAULT_INTEGRATIONS mergeWithHigherPriorityTo options.integrations
    this.context = options.customContext mergeWithHigherPriorityTo context
    this.context = options.externalIds.toJsonObject() mergeWithHigherPriorityTo context
}

internal fun Event.addPersistedValues() {
    this.setAnonymousId()
    this.setUserId()
    this.setTraitsInContext()
}

private fun Event.setAnonymousId() {
    this.anonymousId = userIdentityState.anonymousId
}

private fun Event.setUserId() {
    this.userId = userIdentityState.userId
}

private fun Event.setTraitsInContext() {
    this.context = this.context mergeWithHigherPriorityTo getLatestTraits()
}

private fun Event.getLatestTraits(): JsonObject {
    return userIdentityState.traits
        .takeIf { it.isNotEmpty() }
        ?.let {
            buildJsonObject { put(TRAITS, it) }
        } ?: emptyJsonObject
}

internal fun provideEmptyUserIdentityState() = UserIdentity(String.empty(), String.empty(), emptyJsonObject)
