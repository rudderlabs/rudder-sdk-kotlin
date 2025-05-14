package com.rudderstack.sdk.kotlin.core.internals.models.useridentity

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Traits
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.mergeWithHigherPriorityTo
import kotlinx.serialization.encodeToString

internal class SetUserIdAndTraitsAction(
    private val newUserId: String,
    private val newTraits: Traits,
) : UserIdentity.UserIdentityAction {

    override fun reduce(currentState: UserIdentity): UserIdentity {
        val updatedTraits: Traits = getUpdatedValues(
            previousValue = currentState.traits,
            newValue = this.newTraits,
            mergeWithPriority = { other -> this mergeWithHigherPriorityTo other },
            isUserIdChanged = isUserIdChanged(currentState = currentState)
        )

        LoggerAnalytics.verbose("UserId changed from ${currentState.userId} to $newUserId. Updated traits: $updatedTraits")

        return currentState.copy(userId = newUserId, traits = updatedTraits)
    }

    private fun isUserIdChanged(currentState: UserIdentity): Boolean {
        val previousUserId = currentState.userId
        val newUserId = this.newUserId
        return previousUserId != newUserId
    }
}

private inline fun <T> getUpdatedValues(
    previousValue: T,
    newValue: T,
    mergeWithPriority: T.(T) -> T,
    isUserIdChanged: Boolean
): T = if (isUserIdChanged) newValue else previousValue.mergeWithPriority(newValue)

internal suspend fun UserIdentity.storeUserIdAndTraits(storage: Storage) {
    storage.write(StorageKeys.USER_ID, userId)
    storage.write(StorageKeys.TRAITS, LenientJson.encodeToString(traits))
}
