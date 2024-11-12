package com.rudderstack.kotlin.sdk.internals.models.useridentity

import com.rudderstack.kotlin.sdk.internals.storage.Storage
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys

internal class SetUserIdForAliasEvent(
    private val newId: String,
) : UserIdentity.UserIdentityAction {

    override fun reduce(currentState: UserIdentity): UserIdentity {
        return currentState.copy(userId = newId)
    }
}

internal suspend fun UserIdentity.storeUserId(storage: Storage) {
    storage.write(StorageKeys.USER_ID, userId)
}
