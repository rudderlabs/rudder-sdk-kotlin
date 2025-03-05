package com.rudderstack.sdk.kotlin.core.internals.models.useridentity

import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.generateUUID

internal data object ResetUserIdentityAction : UserIdentity.UserIdentityAction {

    override fun reduce(currentState: UserIdentity): UserIdentity {
        return currentState.copy(
            anonymousId = generateUUID(),
            userId = String.empty(),
            traits = emptyJsonObject,
        )
    }
}

internal suspend fun UserIdentity.resetUserIdentity(storage: Storage) {
    storage.write(StorageKeys.ANONYMOUS_ID, this.anonymousId)
    storage.apply {
        remove(StorageKeys.USER_ID)
        remove(StorageKeys.TRAITS)
    }
}
