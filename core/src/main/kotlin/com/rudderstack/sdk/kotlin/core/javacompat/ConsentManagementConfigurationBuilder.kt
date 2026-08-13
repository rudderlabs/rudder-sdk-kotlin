package com.rudderstack.sdk.kotlin.core.javacompat

import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementConfiguration
import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementConfiguration.Companion.DEFAULT_CONSENT_MANAGEMENT_ENABLED
import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementProvider

/**
 * Builder for ConsentManagementConfiguration instances.
 *
 * This builder class provides Java interop support for configuring consent management.
 */
class ConsentManagementConfigurationBuilder {

    private var enabled: Boolean = DEFAULT_CONSENT_MANAGEMENT_ENABLED
    private var provider: ConsentManagementProvider = ConsentManagementProvider.CUSTOM
    private var allowedConsentIds: List<String> = emptyList()
    private var deniedConsentIds: List<String> = emptyList()

    /**
     * Sets whether consent management is enabled.
     */
    fun setEnabled(enabled: Boolean) = apply {
        this.enabled = enabled
    }

    /**
     * Sets the consent provider.
     */
    fun setProvider(provider: ConsentManagementProvider) = apply {
        this.provider = provider
    }

    /**
     * Sets the consent category IDs the user has granted.
     */
    fun setAllowedConsentIds(allowedConsentIds: List<String>) = apply {
        this.allowedConsentIds = allowedConsentIds
    }

    /**
     * Sets the consent category IDs the user has denied.
     */
    fun setDeniedConsentIds(deniedConsentIds: List<String>) = apply {
        this.deniedConsentIds = deniedConsentIds
    }

    /**
     * Builds the ConsentManagementConfiguration with the configured properties.
     */
    fun build(): ConsentManagementConfiguration {
        return ConsentManagementConfiguration(
            enabled = enabled,
            provider = provider,
            allowedConsentIds = allowedConsentIds,
            deniedConsentIds = deniedConsentIds,
        )
    }
}
