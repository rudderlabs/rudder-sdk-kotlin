package com.rudderstack.kotlin.core.internals.models.useridentity

import com.rudderstack.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.kotlin.core.internals.storage.Storage
import com.rudderstack.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.kotlin.core.internals.utils.empty
import com.rudderstack.kotlin.core.internals.utils.generateUUID

internal class ResetUserIdentityAction(
    private val clearAnonymousId: Boolean,
) : UserIdentity.UserIdentityAction {

    override fun reduce(currentState: UserIdentity): UserIdentity {
        val anonymousId = getOrGenerateAnonymousId(anonymousId = currentState.anonymousId)
        return currentState.copy(
            anonymousId = anonymousId,
            userId = String.empty(),
            traits = emptyJsonObject,
            externalIds = emptyList(),
        )
    }

    private fun getOrGenerateAnonymousId(anonymousId: String): String {
        return if (clearAnonymousId) generateUUID() else anonymousId
    }
}

internal suspend fun UserIdentity.resetUserIdentity(clearAnonymousId: Boolean, storage: Storage) {
    clearAnonymousId.takeIf { it }?.let {
        storage.write(StorageKeys.ANONYMOUS_ID, this.anonymousId)
    }
    storage.apply {
        remove(StorageKeys.USER_ID)
        remove(StorageKeys.TRAITS)
        remove(StorageKeys.EXTERNAL_IDS)
    }
}
