package com.rudderstack.kotlin.sdk.internals.utils

import com.rudderstack.kotlin.sdk.internals.models.useridentity.UserIdentity

/**
 * Resolves and returns the preferred previous identifier for a user based on a defined priority.
 *
 * This function selects the most appropriate identifier for the user by prioritizing non-empty values
 * in the following order:
 * 1. `previousId` – If provided, this ID takes precedence.
 * 2. `userId` – If `previousId` is empty, the current user's ID will be used.
 * 3. `anonymousId` – If both `previousId` and `userId` are empty, this fallback `anonymous ID` is used.
 *
 * @param previousId The potential previous ID to consider, provided as a parameter.
 * @return A non-empty identifier string based on the prioritized values, starting from [previousId],
 *         followed by [userId], and lastly [anonymousId].
 */
internal fun UserIdentity.resolvePreferredPreviousId(previousId: String): String {
    if (previousId.isNotEmpty()) {
        return previousId
    }
    if (this.userId.isNotEmpty()) {
        return this.userId
    }
    return this.anonymousId
}
