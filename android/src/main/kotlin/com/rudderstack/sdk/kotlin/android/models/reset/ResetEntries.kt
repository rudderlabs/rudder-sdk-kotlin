package com.rudderstack.sdk.kotlin.android.models.reset

import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetEntries

/**
 * `ResetEntries` is an Android-specific data class that extends the core ResetEntries model
 * to define which user-related data entries should be reset when performing a reset operation.
 * This class includes Android-specific functionality such as session management.
 *
 * @property anonymousId Flag indicating whether to reset the anonymous ID. Defaults to true.
 * @property userId Flag indicating whether to reset the user ID. Defaults to true.
 * @property traits Flag indicating whether to reset user traits. Defaults to true.
 * @property session Flag indicating whether to reset the session data. Defaults to true.
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
