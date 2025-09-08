package com.rudderstack.sdk.kotlin.core.javacompat

import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetEntries

/**
 * Builder class for creating ResetEntries instances with Java interoperability.
 *
 * This builder provides a fluent API for Java clients to configure reset entry options
 * that override the default reset behavior. Each setter method allows you to specify
 * whether a particular data type should be reset, overriding the default behavior
 * of resetting all data.
 *
 * Example usage:
 * ```java
 * ResetEntries entries = new ResetEntriesBuilder()
 *     .setAnonymousId(true)   // Generate new anonymous ID
 *     .setUserId(false)       // Keep current user ID (override default)
 *     .setTraits(true)        // Clear user traits
 *     .build();
 * ```
 */
open class ResetEntriesBuilder {

    private var anonymousId: Boolean = true
    private var userId: Boolean = true
    private var traits: Boolean = true

    /**
     * Sets whether to reset the anonymous ID, overriding the default behavior.
     *
     * @param reset When true, generates a new anonymous ID (default behavior).
     *              When false, keeps the current anonymous ID (overrides default).
     */
    open fun setAnonymousId(reset: Boolean) = apply {
        anonymousId = reset
    }

    /**
     * Sets whether to reset the user ID, overriding the default behavior.
     *
     * @param reset When true, clears the user ID to empty string (default behavior).
     *              When false, keeps the current user ID (overrides default).
     */
    open fun setUserId(reset: Boolean) = apply {
        userId = reset
    }

    /**
     * Sets whether to reset user traits, overriding the default behavior.
     *
     * @param reset When true, clears user traits to empty JSON object (default behavior).
     *              When false, keeps the current user traits (overrides default).
     */
    open fun setTraits(reset: Boolean) = apply {
        traits = reset
    }

    /**
     * Builds the ResetEntries instance with the configured properties.
     */
    open fun build(): ResetEntries {
        return ResetEntries(
            anonymousId = anonymousId,
            userId = userId,
            traits = traits
        )
    }
}
