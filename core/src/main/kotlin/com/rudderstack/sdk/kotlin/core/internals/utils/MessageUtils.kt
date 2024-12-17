package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.RudderTraits
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
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

internal fun Event.updateIntegrationOptionsAndCustomCustomContext() {
    when (this) {
        is TrackEvent, is ScreenEvent, is GroupEvent, is IdentifyEvent, is AliasEvent -> {
            this.integrations = DEFAULT_INTEGRATIONS mergeWithHigherPriorityTo options.integrations
            this.context = options.customContext mergeWithHigherPriorityTo context
        }
    }
}

internal fun Event.addPersistedValues() {
    this.setAnonymousId()
    this.setUserId()
    this.setTraitsInContext { this.buildTraits() }
    this.setExternalIdInContext()
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

private fun Event.setExternalIdInContext() {
    val externalIds = userIdentityState.externalIds.toJsonObject()
    if (externalIds.isNotEmpty()) {
        this.context = this.context mergeWithHigherPriorityTo externalIds
    }
}

internal fun provideEmptyUserIdentityState() = UserIdentity(String.empty(), String.empty(), emptyJsonObject, emptyList())
