package com.rudderstack.sdk.kotlin.android.javacompat

import com.rudderstack.sdk.kotlin.android.models.reset.ResetEntries
import com.rudderstack.sdk.kotlin.core.javacompat.ResetEntriesBuilder

/**
 * Android-specific builder class for creating [ResetEntries] instances with Java interoperability.
 *
 * This builder extends the core [ResetEntriesBuilder] to provide Android-specific functionality
 * such as control over refreshing the user session. It provides a fluent API for Java clients to configure reset
 * entry options that override the default reset behavior.
 *
 * Example usage:
 * ```java
 * ResetEntries entries = new ResetEntriesBuilder()
 *     .setAnonymousId(true)   // Generate new anonymous ID
 *     .setUserId(false)       // Keep current user ID (override default)
 *     .setTraits(true)        // Clear user traits
 *     .setSession(false)      // Keep current session (override default)
 *     .build();
 * ```
 */
class ResetEntriesBuilder : ResetEntriesBuilder() {

    private var session: Boolean = true

    /**
     * Sets whether to reset the session data, overriding the default behavior (Android-specific).
     *
     * @param reset When true, refreshes the current session (default behavior).
     *              When false, keeps the current session (overrides default).
     */
    fun setSession(reset: Boolean) = apply {
        session = reset
    }

    /**
     * Sets whether to reset the anonymous ID, overriding the default behavior.
     *
     * @param reset When true, generates a new anonymous ID (default behavior).
     *              When false, keeps the current anonymous ID (overrides default).
     */
    override fun setAnonymousId(reset: Boolean) = apply {
        super.setAnonymousId(reset)
    }

    /**
     * Sets whether to reset the user ID, overriding the default behavior.
     *
     * @param reset When true, clears the user ID to empty string (default behavior).
     *              When false, keeps the current user ID (overrides default).
     */
    override fun setUserId(reset: Boolean) = apply {
        super.setUserId(reset)
    }

    /**
     * Sets whether to reset user traits, overriding the default behavior.
     *
     * @param reset When true, clears user traits to empty JSON object (default behavior).
     *              When false, keeps the current user traits (overrides default).
     */
    override fun setTraits(reset: Boolean) = apply {
        super.setTraits(reset)
    }

    /**
     * Builds the [ResetEntries] instance with the configured properties.
     */
    override fun build(): ResetEntries {
        val coreEntries = super.build()

        return ResetEntries(
            anonymousId = coreEntries.anonymousId,
            userId = coreEntries.userId,
            traits = coreEntries.traits,
            session = session
        )
    }
}
