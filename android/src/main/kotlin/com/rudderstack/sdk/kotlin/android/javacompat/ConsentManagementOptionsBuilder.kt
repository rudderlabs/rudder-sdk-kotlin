package com.rudderstack.sdk.kotlin.android.javacompat

import com.rudderstack.sdk.kotlin.android.consent.ConsentManagementOptions

/**
 * Builder for ConsentManagementOptions instances.
 *
 * This builder class provides Java interop support for updating consent at runtime. Naming each
 * list at the call site keeps the granted and denied categories from being transposed.
 *
 * Example usage:
 * ```java
 * ConsentManagementOptions options = new ConsentManagementOptionsBuilder()
 *     .setAllowedConsentIds(Arrays.asList("marketing", "analytics"))
 *     .setDeniedConsentIds(Collections.singletonList("advertising"))
 *     .build();
 *
 * analytics.setConsent(options);
 * ```
 */
class ConsentManagementOptionsBuilder {

    private var allowedConsentIds: List<String> = emptyList()
    private var deniedConsentIds: List<String> = emptyList()

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
     * Builds the ConsentManagementOptions with the configured properties.
     */
    fun build(): ConsentManagementOptions {
        return ConsentManagementOptions(
            allowedConsentIds = allowedConsentIds,
            deniedConsentIds = deniedConsentIds,
        )
    }
}
