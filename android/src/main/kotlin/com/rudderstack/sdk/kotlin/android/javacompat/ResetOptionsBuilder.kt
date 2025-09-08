package com.rudderstack.sdk.kotlin.android.javacompat

import com.rudderstack.sdk.kotlin.android.models.reset.ResetEntries.Companion.DEFAULT_SESSION
import com.rudderstack.sdk.kotlin.android.models.reset.ResetOptions
import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetEntries
import com.rudderstack.sdk.kotlin.core.javacompat.ResetOptionsBuilder
import com.rudderstack.sdk.kotlin.android.models.reset.ResetEntries as AndroidResetEntries

/**
 * Android-specific builder class for creating [ResetOptions] instances with Java interoperability.
 *
 * This builder extends the core [ResetOptionsBuilder] to provide Android-specific functionality
 * such as control over refreshing the user session. It provides a fluent API for Java clients to configure reset
 * options that override the default reset behavior.
 *
 * Example usage:
 * ```java
 * ResetEntries customEntries = new ResetEntriesBuilder()
 *     .setUserId(true)
 *     .setAnonymousId(false)  // Override default - keep anonymous ID
 *     .setTraits(true)
 *     .setSession(false)      // Override default - keep session
 *     .build();
 *
 * ResetOptions options = new ResetOptionsBuilder()
 *     .setEntries(customEntries)
 *     .build();
 *
 * analytics.reset(options);
 * ```
 */
class ResetOptionsBuilder : ResetOptionsBuilder() {

    private var entries: AndroidResetEntries = AndroidResetEntries()

    /**
     * Sets the ResetEntries configuration that overrides the default reset behavior.
     *
     * @param entries [ResetEntries] configuration that specifies which data types
     *                should be reset, allowing selective override of the default
     *                behavior (which resets all data including session).
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
