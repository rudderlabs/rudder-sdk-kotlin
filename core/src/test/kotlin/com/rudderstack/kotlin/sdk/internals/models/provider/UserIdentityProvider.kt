package com.rudderstack.kotlin.sdk.internals.models.provider

import com.rudderstack.kotlin.sdk.internals.models.ExternalId
import com.rudderstack.kotlin.sdk.internals.models.RudderTraits
import com.rudderstack.kotlin.sdk.internals.models.emptyJsonObject
import com.rudderstack.kotlin.sdk.internals.models.useridentity.UserIdentity
import com.rudderstack.kotlin.sdk.internals.utils.ANONYMOUS_ID
import com.rudderstack.kotlin.sdk.internals.utils.empty

fun provideUserIdentityState(
    anonymousId: String = ANONYMOUS_ID,
    userId: String = String.empty(),
    traits: RudderTraits = emptyJsonObject,
    externalIds: List<ExternalId> = emptyList(),
) = UserIdentity(
    anonymousId = anonymousId,
    userId = userId,
    traits = traits,
    externalIds = externalIds,
)
