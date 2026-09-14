package com.rudderstack.sdk.kotlin.android.models.consent

import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.ReservedContextValue
import kotlinx.serialization.json.JsonElement

/**
 * Supplies `context.consentManagement` to the terminal guard, which lives in core and cannot
 * reach the consent state itself.
 *
 * Asserts nothing while consent management is disabled, so the key is not reserved and a legacy
 * customContext injection keeps working.
 */
internal class ConsentContextValue(private val analytics: Analytics) : ReservedContextValue {

    override val overrideAdvice: String =
        "the SDK owns this key while consent management is enabled. Migrate to setConsent()."

    override fun current(): JsonElement? = analytics.consentManagementState.value.takeIf { it.active }?.consentStamp
}
