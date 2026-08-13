package com.rudderstack.sdk.kotlin.core.consent

/**
 * Options for updating consent at runtime via `Analytics.setConsent`.
 *
 * The supplied lists fully replace the current consent state — callers always pass the
 * complete current state, not a delta. An omitted list defaults to empty and clears the
 * corresponding values; an instance with both lists empty clears all consent state and
 * reverts consent resolution to the uninitialized fail-open state.
 *
 * @param allowedConsentIds Consent category IDs the user has granted. Defaults to an empty list.
 * @param deniedConsentIds Consent category IDs the user has denied. Defaults to an empty list.
 */
class ConsentManagementOptions @JvmOverloads constructor(
    val allowedConsentIds: List<String> = emptyList(),
    val deniedConsentIds: List<String> = emptyList(),
)
