package com.rudderstack.sdk.kotlin.core.internals.models.reset

/**
 * `ResetOptions` is a configuration class that provides options for reset operations.
 * This class serves as the base configuration for reset operations across different platforms.
 *
 * @property entries The specific entries to reset, using the ResetEntries configuration.
 *                   Defaults to a new ResetEntries instance with all flags set to true.
 */
open class ResetOptions @JvmOverloads constructor(
    open val entries: ResetEntries = ResetEntries(),
)
