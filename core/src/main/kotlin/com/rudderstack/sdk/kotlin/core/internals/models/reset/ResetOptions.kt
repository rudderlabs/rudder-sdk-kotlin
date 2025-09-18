package com.rudderstack.sdk.kotlin.core.internals.models.reset

/**
 * Configuration class that provides options for reset operations.
 *
 * This class allows you to override the default reset behavior, which normally
 * resets all user data (anonymous ID, user ID, and traits). By providing custom
 * [ResetEntries], you can selectively control which data entries are reset.
 *
 * This class serves as the base configuration for reset operations across different platforms.
 *
 * @property entries The specific entries configuration that overrides the default reset behavior.
 *                   Uses `ResetEntries` to selectively control which data is reset.
 *                   Defaults to a new ResetEntries instance with all flags set to true (full reset).
 */
open class ResetOptions @JvmOverloads constructor(
    open val entries: ResetEntries = ResetEntries(),
)
