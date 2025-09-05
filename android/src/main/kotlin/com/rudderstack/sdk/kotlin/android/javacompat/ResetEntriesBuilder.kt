package com.rudderstack.sdk.kotlin.android.javacompat

import com.rudderstack.sdk.kotlin.android.models.reset.ResetEntries
import com.rudderstack.sdk.kotlin.core.javacompat.ResetEntriesBuilder

/**
 * Builder class for creating ResetEntries instances.
 *
 * This builder class provides Java interop support for configuring reset entries.
 */
class ResetEntriesBuilder : ResetEntriesBuilder() {

    private var session: Boolean = true

    /**
     * Sets whether to reset the session data.
     */
    fun setSession(reset: Boolean) = apply {
        session = reset
    }

    /**
     * Sets whether to reset the anonymous ID.
     */
    override fun setAnonymousId(reset: Boolean) = apply {
        super.setAnonymousId(reset)
    }

    /**
     * Sets whether to reset the user ID.
     */
    override fun setUserId(reset: Boolean) = apply {
        super.setUserId(reset)
    }

    /**
     * Sets whether to reset user traits.
     */
    override fun setTraits(reset: Boolean) = apply {
        super.setTraits(reset)
    }

    /**
     * Builds the ResetEntries instance with the configured properties.
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
