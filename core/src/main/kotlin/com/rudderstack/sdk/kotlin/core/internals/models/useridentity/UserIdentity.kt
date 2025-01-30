package com.rudderstack.sdk.kotlin.core.internals.models.useridentity

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.ExternalId
import com.rudderstack.sdk.kotlin.core.internals.models.RudderTraits
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.FlowAction
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.generateUUID
import com.rudderstack.sdk.kotlin.core.internals.utils.isAnalyticsActive
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
 *
 * @property externalIds A list of external identifiers associated with the user. External IDs are used to track
 * users across different systems and platforms. Each external ID is a key-value pair that includes an ID type
 * and a value.
 */
data class UserIdentity(
    val anonymousId: String,
    val userId: String,
    val traits: RudderTraits,
    val externalIds: List<ExternalId> = emptyList(),
) {

    companion object {

        internal fun initialState(storage: Storage) = UserIdentity(
            anonymousId = storage.readString(StorageKeys.ANONYMOUS_ID, defaultVal = generateUUID()),
            userId = storage.readString(StorageKeys.USER_ID, defaultVal = String.empty()),
            traits = storage.readValuesOrDefault(key = StorageKeys.TRAITS, defaultValue = emptyJsonObject),
            externalIds = storage.readValuesOrDefault(key = StorageKeys.EXTERNAL_IDS, defaultValue = emptyList()),
        )
    }

    internal sealed interface UserIdentityAction : FlowAction<UserIdentity>
}

/**
 * Update or get the stored anonymous ID.
 *
 * The `analyticsInstance.anonymousId` is used to update and get the `anonymousID` value.
 * This ID is typically generated automatically to track users who have not yet been identified
 * (e.g., before they log in or sign up).
 *
 * This can return null if the analytics is shut down.
 *
 * Set the anonymousId:
 * ```kotlin
 * analyticsInstance.anonymousId = "Custom Anonymous ID"
 * ```
 *
 * Get the anonymousId:
 * ```kotlin
 * val anonymousId = analyticsInstance.anonymousId
 * ```
 */
var Analytics.anonymousId: String?
    get() {
        if (!isAnalyticsActive()) return null
        return userIdentityState.value.anonymousId
    }
    set(value) {
        if (!isAnalyticsActive()) return

        value?.let { anonymousId ->
            userIdentityState.dispatch(SetAnonymousIdAction(anonymousId))
            storeAnonymousId()
        }
    }

/**
 * Get the user ID.
 *
 * The `analyticsInstance.userId` is used to get the `userId` value.
 * This ID is assigned when an identify event is made.
 *
 * This can return null if the analytics is shut down.
 *
 * Get the userId:
 * ```kotlin
 * val userId = analyticsInstance.userId
 * ```
 */
val Analytics.userId: String?
    get() {
        if (!isAnalyticsActive()) return null
        return userIdentityState.value.userId
    }

/**
 * Get the user traits.
 *
 * The `analyticsInstance.traits` is used to get the `traits` value.
 * This traits is assigned when an identify event is made.
 *
 * This can return null if the analytics is shut down.
 *
 * Get the traits:
 * ```kotlin
 * val traits = analyticsInstance.traits
 * ```
 */
val Analytics.traits: RudderTraits?
    get() {
        if (!isAnalyticsActive()) return null
        return userIdentityState.value.traits
    }
