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
    open val anonymousId: Boolean = true,
    open val userId: Boolean = true,
    open val traits: Boolean = true,
)
