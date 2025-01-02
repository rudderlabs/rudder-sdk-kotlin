package com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing

import com.rudderstack.sdk.kotlin.android.utils.getBoolean
import com.rudderstack.sdk.kotlin.android.utils.isFalseOrNull
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin

/*
 * This plugin will filter events for each device mode destination based on the integration options set in the event.
 */
internal class IntegrationOptionsPlugin(
    private val key: String
) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    override suspend fun intercept(event: Event): Event? {
        val integrationOptions = event.integrations
        val isDestinationDisabled = integrationOptions.getBoolean(key) == false || (
            integrationOptions.getBoolean("All")
                .isFalseOrNull() && integrationOptions.getBoolean(key).isFalseOrNull()
            )
        return if (isDestinationDisabled) {
            null
        } else {
            event
        }
    }
}
