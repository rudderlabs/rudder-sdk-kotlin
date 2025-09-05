package com.rudderstack.sdk.kotlin.core.javacompat

import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetEntries

/**
 * Builder class for creating ResetEntries instances.
 *
 * This builder class provides Java interop support for configuring reset entries.
 */
open class ResetEntriesBuilder {

    private var anonymousId: Boolean = true
    private var userId: Boolean = true
    private var traits: Boolean = true

    /**
     * Sets whether to reset the anonymous ID.
     */
    open fun setAnonymousId(reset: Boolean) = apply {
        anonymousId = reset
    }

    /**
     * Sets whether to reset the user ID.
     */
    open fun setUserId(reset: Boolean) = apply {
        userId = reset
    }

    /**
     * Sets whether to reset user traits.
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
