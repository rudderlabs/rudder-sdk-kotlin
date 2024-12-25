package com.rudderstack.sdk.kotlin.core.internals.models.useridentity

import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity.UserIdentityAction
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys

internal class SetAnonymousIdAction(
    private val anonymousId: String
) : UserIdentityAction {

    override fun reduce(currentState: UserIdentity): UserIdentity {
        return currentState.copy(anonymousId = anonymousId)
    }
}

internal suspend fun UserIdentity.storeAnonymousId(storage: Storage) {
    storage.write(StorageKeys.ANONYMOUS_ID, anonymousId)
}
