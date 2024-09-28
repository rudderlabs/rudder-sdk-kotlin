package com.rudderstack.core.internals.models

/**
 * Data class representing a user's identity within the application.
 *
 * The `UserIdentity` class is used to manage and store identifiers associated with a user, including both
 * an anonymous ID and a user-specific ID. This can be utilized to track users uniquely across sessions
 * and to distinguish between identified and unidentified users.
 *
 * @property anonymousID The anonymous identifier associated with a user. This ID is typically generated automatically
 * to track users before they have signed up or logged in. It is useful for maintaining state between sessions
 * when the user has not provided explicit identity information.
 *
 * @property userId The unique identifier for an authenticated user. This ID is typically assigned when a user
 * signs up or logs in. If a user is not authenticated, this value can be an empty string or null.
 */
data class UserIdentity(
    var anonymousID: String,
    var userId: String,
)