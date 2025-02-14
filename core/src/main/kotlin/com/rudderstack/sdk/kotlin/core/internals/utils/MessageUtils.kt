package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.RudderTraits
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val NAME = "name"
private const val CATEGORY = "category"
private const val ANONYMOUS_ID = "anonymousId"
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

// TODO("Change the name to addRudderOptionsFields")
internal fun Event.updateIntegrationOptionsAndCustomCustomContext() {
    this.integrations = DEFAULT_INTEGRATIONS mergeWithHigherPriorityTo options.integrations
    this.context = options.customContext mergeWithHigherPriorityTo context
    this.context = options.externalIds.toJsonObject() mergeWithHigherPriorityTo context
}

internal fun Event.addPersistedValues() {
    this.setAnonymousId()
    this.setUserId()
    this.setTraitsInContext { this.buildTraits() }
}

private fun Event.setAnonymousId() {
    this.anonymousId = userIdentityState.anonymousId
}

private fun Event.setUserId() {
    this.userId = userIdentityState.userId
}

private fun Event.setTraitsInContext(getLatestTraits: () -> RudderTraits) {
    getLatestTraits().let { latestTraits ->
        this.context = this.context mergeWithHigherPriorityTo latestTraits
    }
}

private fun Event.buildTraits(): RudderTraits {
    val latestAnonymousId = userIdentityState.anonymousId
    val latestTraits = userIdentityState.traits

    return (latestTraits mergeWithHigherPriorityTo getDefaultTraits(latestAnonymousId))
        .let { updatedTraits ->
            buildJsonObject {
                put(TRAITS, updatedTraits)
            }
        }
}

private fun getDefaultTraits(anonymousId: String): RudderTraits = buildJsonObject {
    put(ANONYMOUS_ID, anonymousId)
}

internal fun provideEmptyUserIdentityState() = UserIdentity(String.empty(), String.empty(), emptyJsonObject)
