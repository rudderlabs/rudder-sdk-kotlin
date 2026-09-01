package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.consent.ConsentManagementOptions
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin

/**
 * A sample pattern for bridging a Consent Management Platform into the SDK. This is example
 * code, not SDK API — copy it into your project and adapt it to your CMP.
 *
 * The plugin never modifies the event context. The SDK owns `context.consentManagement` and
 * stamps it from the state recorded by [Analytics.setConsent]; a plugin writing that key is
 * overwritten by the SDK and logs a warning.
 *
 * Add the plugin like this:
 * ```
 * analytics.add(ConsentPlugin(provider = myCmpAdapter))
 * ```
 * Adding it pushes whatever the CMP already knows, then keeps the SDK in sync as the user
 * changes their choices.
 *
 * @param provider The CMP adapter this plugin reads consent choices from.
 */
class ConsentPlugin(
    private val provider: ConsentCategoryProvider
) : Plugin {

    // Never intercepts events — it only reacts to the CMP and calls setConsent.
    override val pluginType: Plugin.PluginType = Plugin.PluginType.Utility

    override lateinit var analytics: Analytics

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        provider.onConsentChanged = { pushCurrentConsent() }
        pushCurrentConsent()
    }

    override fun teardown() {
        provider.onConsentChanged = null
    }

    /**
     * Hands the CMP's current choices to the SDK. The new lists fully replace the previous
     * consent state and apply from the next event onward.
     */
    private fun pushCurrentConsent() {
        analytics.setConsent(
            ConsentManagementOptions(
                allowedConsentIds = provider.allowedConsentIds,
                deniedConsentIds = provider.deniedConsentIds,
            )
        )
    }
}

/**
 * The slice of a Consent Management Platform that [ConsentPlugin] depends on.
 *
 * Adapt this to whichever CMP the app uses — the plugin needs only the two ID lists and a
 * notification for when the user changes their choices.
 */
interface ConsentCategoryProvider {

    /** Consent category IDs the user has granted. */
    val allowedConsentIds: List<String>

    /** Consent category IDs the user has denied. */
    val deniedConsentIds: List<String>

    /** Invoked by the CMP whenever the user's choices change. */
    var onConsentChanged: (() -> Unit)?
}
