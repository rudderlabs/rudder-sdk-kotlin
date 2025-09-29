package com.rudderstack.sdk.kotlin.core.internals.models.reset

/**
 * Configuration class that defines which user-related data entries should be reset
 * when performing a reset operation.
 *
 * This class allows you to override the default reset behavior (which resets all data)
 * by selectively controlling which specific data entries are reset. Each boolean flag
 * overrides the default behavior for that specific data type.
 *
 * This class serves as the base configuration for reset operations across different platforms.
 *
 * @property anonymousId Flag that overrides the default behavior for anonymous ID reset.
 *                       When true, generates a new anonymous ID (default: true).
 * @property userId Flag that overrides the default behavior for user ID reset.
 *                  When true, clears the user ID to empty string (default: true).
 * @property traits Flag that overrides the default behavior for user traits reset.
 *                  When true, clears user traits to empty JSON object (default: true).
 */
open class ResetEntries @JvmOverloads constructor(
    open val anonymousId: Boolean = DEFAULT_ANONYMOUS_ID,
    open val userId: Boolean = DEFAULT_USER_ID,
    open val traits: Boolean = DEFAULT_TRAITS,
) {

    companion object {
        /**
         * Default status of resetting the anonymousId value.
         */
        const val DEFAULT_ANONYMOUS_ID = true

        /**
         * Default status of resetting the userId value.
         */
        const val DEFAULT_USER_ID = true

        /**
         * Default status of resetting the traits.
         */
        const val DEFAULT_TRAITS = true
    }
}
