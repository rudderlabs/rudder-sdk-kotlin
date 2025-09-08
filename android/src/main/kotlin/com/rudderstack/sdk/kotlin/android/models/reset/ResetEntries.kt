package com.rudderstack.sdk.kotlin.android.models.reset

import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetEntries

/**
 * Android-specific configuration class that extends the [ResetEntries] model
 * to define which user-related data entries should be reset when performing a reset operation.
 *
 * This class allows you to override the default reset behavior (which resets all data)
 * by selectively controlling which specific data entries are reset. Each boolean flag
 * overrides the default behavior for that specific data type.
 *
 * This class includes Android-specific functionality such as user session.
 *
 * @property anonymousId Flag that overrides the default behavior for anonymous ID reset.
 *                       When true, generates a new anonymous ID (default: true).
 * @property userId Flag that overrides the default behavior for user ID reset.
 *                  When true, clears the user ID to empty string (default: true).
 * @property traits Flag that overrides the default behavior for user traits reset.
 *                  When true, clears user traits to empty JSON object (default: true).
 * @property session Flag that overrides the default behavior for session reset (Android-specific).
 *                   When true, refreshes the current session (default: true).
 */
data class ResetEntries @JvmOverloads constructor(
    override val anonymousId: Boolean = DEFAULT_ANONYMOUS_ID,
    override val userId: Boolean = DEFAULT_USER_ID,
    override val traits: Boolean = DEFAULT_TRAITS,
    val session: Boolean = DEFAULT_SESSION,
) : ResetEntries() {

    companion object {

        /**
         * Default status of resetting the user session.
         */
        internal const val DEFAULT_SESSION = true
    }
}
