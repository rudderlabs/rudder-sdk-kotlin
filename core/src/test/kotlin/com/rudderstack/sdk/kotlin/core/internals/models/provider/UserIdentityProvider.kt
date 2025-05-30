package com.rudderstack.sdk.kotlin.core.internals.models.provider

import com.rudderstack.sdk.kotlin.core.ANONYMOUS_ID
import com.rudderstack.sdk.kotlin.core.internals.models.Traits
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.utils.empty

fun provideUserIdentityState(
    anonymousId: String = ANONYMOUS_ID,
    userId: String = String.empty(),
    traits: Traits = emptyJsonObject,
) = UserIdentity(
    anonymousId = anonymousId,
    userId = userId,
    traits = traits,
)
