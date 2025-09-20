package com.rudderstack.sdk.kotlin.core.javacompat

import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetEntries
import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetOptions

/**
 * Builder class for creating ResetOptions instances with Java interoperability.
 *
 * This builder provides a fluent API for Java clients to configure reset options
 * that override the default reset behavior. Use this builder to specify custom
 * [ResetEntries] that selectively control which data entries are reset.
 *
 * Example usage:
 * ```java
 * ResetEntries customEntries = new ResetEntriesBuilder()
 *     .setUserId(true)
 *     .setAnonymousId(false)  // Override default - keep anonymous ID
 *     .setTraits(true)
 *     .build();
 *
 * ResetOptions options = new ResetOptionsBuilder()
 *     .setEntries(customEntries)
 *     .build();
 *
 * analytics.reset(options);
 * ```
 */
open class ResetOptionsBuilder {

    private var entries: ResetEntries = ResetEntries()

    /**
     * Sets the reset entries configuration that overrides the default reset behavior.
     *
     * @param entries [ResetEntries] configuration that specifies which data types
     *                should be reset, allowing selective override of the default
     *                behavior (which resets all data).
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
