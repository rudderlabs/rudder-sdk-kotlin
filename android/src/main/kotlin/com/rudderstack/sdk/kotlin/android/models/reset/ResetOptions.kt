package com.rudderstack.sdk.kotlin.android.models.reset

import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetOptions

/**
 * `ResetOptions` is an Android-specific data class that extends the core ResetOptions model
 * to configure the reset operation for user data. This class provides Android-specific
 * reset functionality and options.
 *
 * @property entries The specific entries to reset, using the Android-specific `ResetEntries` configuration.
 *                   Defaults to a new `ResetEntries` instance with all flags set to true.
 */
data class ResetOptions @JvmOverloads constructor(
    override val entries: ResetEntries = ResetEntries(),
) : ResetOptions()
