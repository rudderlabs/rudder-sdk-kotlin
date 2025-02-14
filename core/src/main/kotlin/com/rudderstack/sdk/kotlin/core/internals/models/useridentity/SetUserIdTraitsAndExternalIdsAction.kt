package com.rudderstack.sdk.kotlin.core.internals.models.useridentity

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.RudderTraits
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.mergeWithHigherPriorityTo
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

// TODO("Change it to SetUserIdAndTraitsAction")
internal class SetUserIdTraitsAndExternalIdsAction(
    private val newUserId: String,
    private val newTraits: RudderTraits,
    private val analytics: Analytics,
) : UserIdentity.UserIdentityAction {

    override fun reduce(currentState: UserIdentity): UserIdentity {
        val isUserIdChanged = isUserIdChanged(currentState = currentState)

        val updatedTraits: RudderTraits = getUpdatedValues(
            previousValue = currentState.traits,
            newValue = this.newTraits,
            mergeWithPriority = { other -> this mergeWithHigherPriorityTo other },
            isUserIdChanged = isUserIdChanged
        )

        resetValuesIfUserIdChanged(isUserIdChanged = isUserIdChanged)

        return currentState.copy(userId = newUserId, traits = updatedTraits)
    }

    private fun isUserIdChanged(currentState: UserIdentity): Boolean {
        val previousUserId = currentState.userId
        val newUserId = this.newUserId
        return previousUserId != newUserId
    }

    private fun resetValuesIfUserIdChanged(isUserIdChanged: Boolean) {
        if (isUserIdChanged) {
            analytics.analyticsScope.launch(analytics.storageDispatcher) {
                resetUserIdTraitsAndExternalIds()
            }
        }
    }

    // TODO("Change it to resetUserIdAndTraitsActions)
    private suspend fun resetUserIdTraitsAndExternalIds() {
        analytics.storage.let {
            it.remove(StorageKeys.USER_ID)
            it.remove(StorageKeys.TRAITS)
        }
    }
}

private inline fun <T> getUpdatedValues(
    previousValue: T,
    newValue: T,
    mergeWithPriority: T.(T) -> T,
    isUserIdChanged: Boolean
): T = if (isUserIdChanged) newValue else previousValue.mergeWithPriority(newValue)

// TODO("Change it to .storeUserIdAndTraits)
internal suspend fun UserIdentity.storeUserIdTraitsAndExternalIds(storage: Storage) {
    storage.write(StorageKeys.USER_ID, userId)
    storage.write(StorageKeys.TRAITS, LenientJson.encodeToString(traits))
}
