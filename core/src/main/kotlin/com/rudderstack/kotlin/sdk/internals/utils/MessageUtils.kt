package com.rudderstack.kotlin.sdk.internals.utils

import com.rudderstack.kotlin.sdk.internals.models.GroupEvent
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.models.RudderTraits
import com.rudderstack.kotlin.sdk.internals.models.emptyJsonObject
import com.rudderstack.kotlin.sdk.internals.models.useridentity.UserIdentity
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val NAME = "name"
private const val CATEGORY = "category"
private const val ANONYMOUS_ID = "anonymousId"
private const val USER_ID = "userId"
private const val ID = "id"
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

internal fun Message.setGroupEventTraits() {
    if (this is GroupEvent) {
        this.traits = getUpdatedTraitsWithAnonymousId(anonymousId = anonymousId, traits = this.traits)
    }
}

private fun getUpdatedTraitsWithAnonymousId(anonymousId: String, traits: RudderTraits): RudderTraits {
    return if (traits.isNotEmpty()) {
        val traitsWithAnonymousId = getDefaultTraits(anonymousId)
        traits mergeWithHigherPriorityTo traitsWithAnonymousId
    } else {
        traits
    }
}

internal fun Message.addPersistedValues() {
    this.setAnonymousId()
    this.setUserId()
    this.setTraitsInContext { this.buildTraits() }
    this.setExternalIdInContext()
}

private fun Message.setAnonymousId() {
    this.anonymousId = userIdentityState.anonymousId
}

private fun Message.setUserId() {
    this.userId = userIdentityState.userId
}

private fun Message.setTraitsInContext(getLatestTraits: () -> RudderTraits) {
    getLatestTraits().let { latestTraits ->
        this.context = this.context mergeWithHigherPriorityTo latestTraits
    }
}

private fun Message.buildTraits(): RudderTraits {
    val latestAnonymousId = userIdentityState.anonymousId
    val latestUserId = userIdentityState.userId
    val latestTraits = userIdentityState.traits

    return (getDefaultTraits(latestAnonymousId) mergeWithHigherPriorityTo getUserIdAddedTraits(latestUserId))
        .let { defaultTraits ->
            latestTraits mergeWithHigherPriorityTo defaultTraits
        }.let { updatedTraits ->
            buildJsonObject {
                put(TRAITS, updatedTraits)
            }
        }
}

private fun getDefaultTraits(anonymousId: String): RudderTraits = buildJsonObject {
    put(ANONYMOUS_ID, anonymousId)
}

private fun getUserIdAddedTraits(userId: String): RudderTraits = buildJsonObject {
    if (userId.isNotEmpty()) {
        put(USER_ID, userId)
        put(ID, userId)
    }
}

private fun Message.setExternalIdInContext() {
    val externalIds = userIdentityState.externalIds.toJsonObject()
    if (externalIds.isNotEmpty()) {
        this.context = this.context mergeWithHigherPriorityTo externalIds
    }
}

internal fun provideEmptyUserIdentityState() = UserIdentity(String.empty(), String.empty(), emptyJsonObject, emptyList())
