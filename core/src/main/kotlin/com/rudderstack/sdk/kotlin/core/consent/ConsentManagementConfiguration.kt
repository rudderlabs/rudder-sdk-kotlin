package com.rudderstack.sdk.kotlin.core.consent

/**
 * Configuration for consent management.
 *
 * @param enabled Flag to enable or disable consent management. Defaults to `false`.
 * @param provider The consent provider. Currently only [ConsentManagementProvider.CUSTOM] is supported.
 * @param allowedConsentIds Consent category IDs the user has granted. Defaults to an empty list.
 * @param deniedConsentIds Consent category IDs the user has denied. Defaults to an empty list.
 */
data class ConsentManagementConfiguration @JvmOverloads constructor(
    val enabled: Boolean = DEFAULT_CONSENT_MANAGEMENT_ENABLED,
    val provider: ConsentManagementProvider = ConsentManagementProvider.CUSTOM,
    val allowedConsentIds: List<String> = emptyList(),
    val deniedConsentIds: List<String> = emptyList(),
) {
    companion object {
        internal const val DEFAULT_CONSENT_MANAGEMENT_ENABLED = false
    }
}

/**
 * Supported consent management providers.
 *
 * @param value The wire value stamped into `context.consentManagement.provider`.
 */
enum class ConsentManagementProvider(val value: String) {
    CUSTOM("custom"),
}
