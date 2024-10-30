package com.rudderstack.kotlin.sdk.internals.models.useridentity

import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.ExternalId
import com.rudderstack.kotlin.sdk.internals.models.RudderTraits
import com.rudderstack.kotlin.sdk.internals.storage.Storage
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys
import com.rudderstack.kotlin.sdk.internals.utils.LenientJson
import com.rudderstack.kotlin.sdk.internals.utils.mergeWithHigherPriorityTo
import com.rudderstack.kotlin.sdk.internals.utils.toJson
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

internal class SetUserIdTraitsAndExternalIdsAction(
    private val newUserId: String,
    private val newTraits: RudderTraits,
    private val newExternalIds: List<ExternalId>,
    private val analytics: Analytics,
) : UserIdentity.UserIdentityAction {

    override fun reduce(currentState: UserIdentity): UserIdentity {
        val isUserIdChanged = isUserIdChanged(currentState = currentState)

        val updatedTraits: RudderTraits = getUpdatedValues(
            currentStateValue = currentState.traits,
            newValue = this.newTraits,
            mergeWithPriority = { other -> this mergeWithHigherPriorityTo other },
            isUserIdChanged = isUserIdChanged
        )

        val updatedExternalIds: List<ExternalId> = getUpdatedValues(
            currentStateValue = currentState.externalIds,
            newValue = this.newExternalIds,
            mergeWithPriority = { other -> this mergeWithHigherPriorityTo other },
            isUserIdChanged = isUserIdChanged
        )

        resetValuesIfUserIdChanged(isUserIdChanged = isUserIdChanged)

        return currentState.copy(userId = newUserId, traits = updatedTraits, externalIds = updatedExternalIds)
    }

    private fun isUserIdChanged(currentState: UserIdentity): Boolean {
        val oldUserId = currentState.userId
        val newUserId = this.newUserId
        return oldUserId != newUserId
    }

    private fun resetValuesIfUserIdChanged(isUserIdChanged: Boolean) {
        if (isUserIdChanged) {
            analytics.analyticsScope.launch(analytics.storageDispatcher) {
                resetUserIdTraitsAndExternalIds()
            }
        }
    }

    private suspend fun resetUserIdTraitsAndExternalIds() {
        analytics.configuration.storage.let {
            it.remove(StorageKeys.USER_ID)
            it.remove(StorageKeys.TRAITS)
            it.remove(StorageKeys.EXTERNAL_IDS)
        }
    }
}

private inline fun <T> getUpdatedValues(
    currentStateValue: T,
    newValue: T,
    mergeWithPriority: T.(T) -> T,
    isUserIdChanged: Boolean
): T = if (isUserIdChanged) newValue else currentStateValue.mergeWithPriority(newValue)

internal suspend fun UserIdentity.storeUserIdTraitsAndExternalIds(storage: Storage) {
    storage.write(StorageKeys.USER_ID, userId)
    storage.write(StorageKeys.TRAITS, LenientJson.encodeToString(traits))
    storage.write(StorageKeys.EXTERNAL_IDS, externalIds.toJson().toString())
}
