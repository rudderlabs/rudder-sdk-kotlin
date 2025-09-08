package com.rudderstack.sdk.kotlin.core.internals.models.useridentity

import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetEntries
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.generateUUID

internal class ResetUserIdentityAction(private val entries: ResetEntries) : UserIdentity.UserIdentityAction {

    override fun reduce(currentState: UserIdentity): UserIdentity {
        return with(entries) {
            currentState.copy(
                anonymousId = when {
                    anonymousId -> generateUUID()
                    else -> currentState.anonymousId
                },
                userId = when {
                    userId -> String.empty()
                    else -> currentState.userId
                },
                traits = when {
                    traits -> emptyJsonObject
                    else -> currentState.traits
                }
            )
        }
    }
}

internal suspend fun UserIdentity.resetUserIdentity(storage: Storage, entries: ResetEntries) {
    storage.apply {
        if (entries.anonymousId) {
            write(StorageKeys.ANONYMOUS_ID, this@resetUserIdentity.anonymousId)
        }
        if (entries.userId) {
            remove(StorageKeys.USER_ID)
        }
        if (entries.traits) {
            remove(StorageKeys.TRAITS)
        }
    }
}
