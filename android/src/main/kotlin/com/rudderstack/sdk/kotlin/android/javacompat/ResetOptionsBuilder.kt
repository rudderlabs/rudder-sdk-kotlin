package com.rudderstack.sdk.kotlin.android.javacompat

import com.rudderstack.sdk.kotlin.android.models.reset.ResetEntries.Companion.DEFAULT_SESSION
import com.rudderstack.sdk.kotlin.android.models.reset.ResetOptions
import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetEntries
import com.rudderstack.sdk.kotlin.core.javacompat.ResetOptionsBuilder
import com.rudderstack.sdk.kotlin.android.models.reset.ResetEntries as AndroidResetEntries

/**
 * Builder class for creating ResetOptions instances.
 *
 * This builder class provides Java interop support for configuring reset options.
 */
class ResetOptionsBuilder : ResetOptionsBuilder() {

    private var entries: AndroidResetEntries = AndroidResetEntries()

    /**
     * Sets the reset entries configuration.
     */
    override fun setEntries(entries: ResetEntries) = apply {
        this.entries = AndroidResetEntries(
            anonymousId = entries.anonymousId,
            userId = entries.userId,
            traits = entries.traits,
            session = (entries as? AndroidResetEntries)?.session ?: DEFAULT_SESSION
        )
    }

    /**
     * Builds the ResetOptions instance with the configured properties.
     */
    override fun build(): ResetOptions {
        return ResetOptions(
            entries = entries
        )
    }
}
