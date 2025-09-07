package com.rudderstack.sdk.kotlin.core.internals.models.useridentity

import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetOptions
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.generateUUID

internal class ResetUserIdentityAction(val options: ResetOptions) : UserIdentity.UserIdentityAction {

    override fun reduce(currentState: UserIdentity): UserIdentity {
        return currentState.copy(
            anonymousId = if (options.entries.anonymousId) {
                generateUUID()
            } else {
                currentState.anonymousId
            },
            userId = if (options.entries.userId) {
                String.empty()
            } else {
                currentState.userId
            },
            traits = if (options.entries.traits) {
                emptyJsonObject
            } else {
                currentState.traits
            }
        )
    }
}

internal suspend fun UserIdentity.resetUserIdentity(storage: Storage, options: ResetOptions) {
    if (options.entries.anonymousId) {
        storage.write(StorageKeys.ANONYMOUS_ID, this.anonymousId)
    }
    if (options.entries.userId) {
        storage.remove(StorageKeys.USER_ID)
    }
    if (options.entries.traits) {
        storage.remove(StorageKeys.TRAITS)
    }
}
