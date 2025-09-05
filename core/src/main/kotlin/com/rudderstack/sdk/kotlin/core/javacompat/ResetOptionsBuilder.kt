package com.rudderstack.sdk.kotlin.core.javacompat

import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetEntries
import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetOptions

/**
 * Builder class for creating ResetOptions instances.
 *
 * This builder class provides Java interop support for configuring reset options.
 */
open class ResetOptionsBuilder {

    private var entries: ResetEntries = ResetEntries()

    /**
     * Sets the reset entries configuration.
     */
    open fun setEntries(entries: ResetEntries) = apply {
        this.entries = entries
    }

    /**
     * Builds the ResetOptions instance with the configured properties.
     */
    open fun build(): ResetOptions {
        return ResetOptions(
            entries = entries
        )
    }
}
