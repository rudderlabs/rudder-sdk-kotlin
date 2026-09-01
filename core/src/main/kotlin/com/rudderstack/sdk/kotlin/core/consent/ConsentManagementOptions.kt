package com.rudderstack.sdk.kotlin.core.consent

/**
 * Options for updating consent at runtime via `Analytics.setConsent`.
 *
 * The supplied lists fully replace the current consent state — callers always pass the
 * complete current state, not a delta. An omitted list defaults to empty and clears the
 * corresponding values; an instance with both lists empty is rejected and leaves the
 * current consent state unchanged. Updates apply only while consent management is
 * enabled; otherwise the existing state is preserved.
 *
 * @param allowedConsentIds Consent category IDs the user has granted. Defaults to an empty list.
 * @param deniedConsentIds Consent category IDs the user has denied. Defaults to an empty list.
 */
class ConsentManagementOptions @JvmOverloads constructor(
    val allowedConsentIds: List<String> = emptyList(),
    val deniedConsentIds: List<String> = emptyList(),
)
