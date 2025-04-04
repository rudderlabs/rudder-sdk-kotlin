package com.rudderstack.sdk.kotlin.core.internals.models.useridentity

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys

internal class SetUserIdForAliasEvent(
    private val newId: String,
) : UserIdentity.UserIdentityAction {

    override fun reduce(currentState: UserIdentity): UserIdentity {
        LoggerAnalytics.verbose("UserId changed from ${currentState.userId} to $newId.")
        return currentState.copy(userId = newId)
    }
}

internal suspend fun UserIdentity.storeUserId(storage: Storage) {
    storage.write(StorageKeys.USER_ID, userId)
}
