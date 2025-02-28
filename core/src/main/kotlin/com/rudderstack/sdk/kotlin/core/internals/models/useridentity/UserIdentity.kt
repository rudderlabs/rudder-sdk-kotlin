package com.rudderstack.sdk.kotlin.core.internals.models.useridentity

import com.rudderstack.sdk.kotlin.core.internals.models.RudderTraits
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.FlowAction
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.generateUUID
import com.rudderstack.sdk.kotlin.core.internals.utils.readValuesOrDefault

/**
 * Data class representing a user's identity within the application.
 *
 * The `UserIdentity` class is used to manage and store identifiers associated with a user, including both
 * an anonymous ID and a user-specific ID. This can be utilized to track users uniquely across sessions
 * and to distinguish between identified and unidentified users.
 *
 * @property anonymousId The anonymous identifier associated with a user. This ID is typically generated automatically
 * to track users before they have signed up or logged in. It is useful for maintaining state between sessions
 * when the user has not provided explicit identity information.
 *
 * @property userId The unique identifier for an authenticated user. This ID is typically assigned when a user
 * signs up or logs in. If a user is not authenticated, this value can be an empty string or null.
 *
 * @property traits A collection of traits associated with the user. Traits are key-value pairs that can be used
 * to store additional information about a user, such as their name, email, or other properties.
 */
data class UserIdentity(
    val anonymousId: String,
    val userId: String,
    val traits: RudderTraits,
) {

    companion object {

        internal fun initialState(storage: Storage) = UserIdentity(
            anonymousId = storage.readString(StorageKeys.ANONYMOUS_ID, defaultVal = generateUUID()),
            userId = storage.readString(StorageKeys.USER_ID, defaultVal = String.empty()),
            traits = storage.readValuesOrDefault(key = StorageKeys.TRAITS, defaultValue = emptyJsonObject),
        )
    }

    internal suspend fun storeAnonymousId(storage: Storage) {
        storage.write(StorageKeys.ANONYMOUS_ID, anonymousId)
    }

    internal sealed interface UserIdentityAction : FlowAction<UserIdentity>
}
