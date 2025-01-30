package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginChain
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

internal const val MAX_QUEUE_SIZE = 1000
internal const val FIRST_INDEX = 0

/*
 * This plugin will queue the events till the sourceConfig is fetched and
 * will host all the device mode integration plugins in its PluginChain instance.
 */
internal class IntegrationsManagementPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Destination

    override lateinit var analytics: Analytics

    private val integrationPluginChain = PluginChain()

    private val queuedEventsChannel: Channel<Event> = Channel(MAX_QUEUE_SIZE)

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        integrationPluginChain.analytics = analytics
        analytics.withIntegrationsDispatcher {
            analytics.sourceConfigState
                .filter { it.source.isSourceEnabled }
                .collectIndexed { index, sourceConfig ->
                    integrationPluginChain.applyClosure { plugin ->
                        if (plugin is IntegrationPlugin) {
                            plugin.findAndInitDestination(sourceConfig)
                        }
                    }

                    if (index == FIRST_INDEX) {
                        processEvents()
                    }
                }
        }
    }

    override suspend fun intercept(event: Event): Event {
        LoggerAnalytics.debug("IntegrationsManagementPlugin: queueing event")
        val queuedEventResult = queuedEventsChannel.trySend(event)

        if (queuedEventResult.isFailure) {
            dropOldestAndSend(event)
        }

        return event
    }

    override fun teardown() {
        integrationPluginChain.removeAll()
        queuedEventsChannel.cancel()
    }

    internal fun addIntegration(plugin: IntegrationPlugin) {
        integrationPluginChain.add(plugin)
        analytics.withIntegrationsDispatcher {
            if (plugin.getDestinationInstance() == null) {
                plugin.findAndInitDestination(analytics.sourceConfigState.value)
            }
        }
    }

    internal fun removeIntegration(plugin: IntegrationPlugin) {
        integrationPluginChain.remove(plugin)
    }

    internal fun reset() {
        analytics.withIntegrationsDispatcher {
            integrationPluginChain.applyClosure { plugin ->
                if (plugin is IntegrationPlugin) {
                    if (plugin.isDestinationReady) {
                        plugin.reset()
                    } else {
                        LoggerAnalytics.debug(
                            "IntegrationsManagementPlugin: Integrations are " +
                                "not initialised yet. Reset discarded."
                        )
                    }
                }
            }
        }
    }

    internal fun flush() {
        analytics.withIntegrationsDispatcher {
            integrationPluginChain.applyClosure { plugin ->
                if (plugin is IntegrationPlugin) {
                    if (plugin.isDestinationReady) {
                        plugin.flush()
                    } else {
                        LoggerAnalytics.debug(
                            "IntegrationsManagementPlugin: Integrations are " +
                                "not initialised yet. Flush discarded."
                        )
                    }
                }
            }
        }
    }

    private fun dropOldestAndSend(event: Event) {
        queuedEventsChannel.tryReceive()
        queuedEventsChannel.trySend(event)
    }

    private suspend fun processEvents() {
        for (event in queuedEventsChannel) {
            integrationPluginChain.process(event)
        }
    }
}

private fun Analytics.withIntegrationsDispatcher(block: suspend () -> Unit) {
    analyticsScope.launch(integrationsDispatcher) {
        block()
    }
}
