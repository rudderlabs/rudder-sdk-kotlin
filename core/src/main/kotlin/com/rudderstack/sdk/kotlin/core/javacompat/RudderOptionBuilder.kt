package com.rudderstack.sdk.kotlin.core.javacompat

import com.rudderstack.sdk.kotlin.core.internals.models.ExternalId
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.javacompat.JsonInteropHelper.fromMap
import kotlinx.serialization.json.JsonObject

/**
 * A Java-friendly wrapper for constructing RudderOption instances.
 *
 * This class provides a builder pattern and Java-compatible methods to construct
 * and manipulate RudderOption instances from Java code.
 */
class RudderOptionBuilder {

    private var integrations: JsonObject = emptyJsonObject
    private var externalIds: List<ExternalId> = emptyList()
    private var customContext: JsonObject = emptyJsonObject

    /**
     * Sets the integrations for this RudderOption.
     *
     * @param integrations A map representing the integrations to be set
     * @return This RudderOptionBuilder instance for method chaining
     */
    fun setIntegrations(integrations: Map<String, Any>): RudderOptionBuilder {
        this.integrations = fromMap(integrations)
        return this
    }

    /**
     * Sets the external IDs for this RudderOption.
     *
     * @param externalIds A list of ExternalId objects to be added
     * @return This RudderOptionBuilder instance for method chaining
     */
    fun setExternalId(externalIds: List<ExternalId>): RudderOptionBuilder {
        this.externalIds = externalIds
        return this
    }

    /**
     * Sets the custom context for this RudderOption.
     *
     * @param customContext A map representing the custom context
     * @return This RudderOptionBuilder instance for method chaining
     */
    fun setCustomContext(customContext: Map<String, Any>): RudderOptionBuilder {
        this.customContext = fromMap(customContext)
        return this
    }

    /**
     * Builds and returns a RudderOption instance with the configured values.
     *
     * @return A RudderOption instance
     */
    fun build(): RudderOption {
        return RudderOption(
            integrations = integrations,
            externalIds = externalIds,
            customContext = customContext
        )
    }
}
