package com.rudderstack.sdk.kotlin.core.internals.models.reset

/**
 * `ResetEntries` is a configuration class that defines which user-related data entries
 * should be reset when performing a reset operation. This class serves as the base
 * configuration for reset operations across different platforms.
 *
 * @property anonymousId Flag indicating whether to reset the anonymous ID. Defaults to true.
 * @property userId Flag indicating whether to reset the user ID. Defaults to true.
 * @property traits Flag indicating whether to reset user traits. Defaults to true.
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
