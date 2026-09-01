package com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing

import com.rudderstack.sdk.kotlin.android.utils.findDestination
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.consent.ConsentResolver
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/**
 * A plugin to drop events for a destination while it is denied by user consent.
 *
 * The consent state is read live per event, so a consent change applies to the very
 * next event. Evaluation fails open: without consent data or destination consent
 * configuration, every event passes through.
 */
internal class ConsentGatePlugin(private val key: String) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    @Volatile
    private var destinationConfig: JsonObject? = null

    private var configJob: Job? = null

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        configJob = listenForConfigChanges()
    }

    override fun teardown() {
        configJob?.cancel()
    }

    override suspend fun intercept(event: Event): Event? {
        return if (ConsentResolver.resolve(analytics.consentManagementState.value, destinationConfig)) {
            event
        } else {
            analytics.logger.debug(
                "ConsentGatePlugin: Dropped event for destination: $key (messageId=${event.messageId})"
            )
            null
        }
    }

    private fun listenForConfigChanges(): Job = analytics.analyticsScope.launch {
        analytics.sourceConfigState
            .observeDispatched()
            .collect { sourceConfig ->
                destinationConfig = findDestination(sourceConfig, key)?.destinationConfig
            }
    }
}
