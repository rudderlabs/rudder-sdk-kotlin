package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin

/**
 * A plugin that sets a given [anonymousId] in the event payload for every event.
 *
 * **Note**: The `anonymousId` fetched using [Analytics.anonymousId] would be different from the `anonymousId` set here.
 */
class SetAnonymousIdPlugin(
    private val anonymousId: String
) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    override suspend fun intercept(event: Event): Event = replaceAnonymousId(event)

    private fun replaceAnonymousId(event: Event): Event {
        LoggerAnalytics.debug("SetAnonymousIdPlugin: Replacing anonymousId: $anonymousId in the event payload")

        event.anonymousId = anonymousId

        return event
    }
}
