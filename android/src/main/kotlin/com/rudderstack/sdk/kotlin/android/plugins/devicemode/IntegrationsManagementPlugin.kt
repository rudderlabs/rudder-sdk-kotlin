package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginChain
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.dropInitialState
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

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Terminal

    override lateinit var analytics: Analytics

    private val integrationPluginChain = PluginChain()

    private val queuedEventsChannel: Channel<Event> = Channel(MAX_QUEUE_SIZE)

    private val sourceConfig: SourceConfig
        get() = analytics.sourceConfigState.value

    private var isSourceEnabledFetchedOnce = false

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        integrationPluginChain.analytics = analytics
        analytics.withIntegrationsDispatcher {
            analytics.sourceConfigState
                .dropInitialState()
                .filter { it.source.isSourceEnabled }
                .collectIndexed { index, sourceConfig ->
                    integrationPluginChain.applyClosure { plugin ->
                        if (plugin is IntegrationPlugin) {
                            plugin.initDestination(sourceConfig)
                        }
                    }

                    if (index == FIRST_INDEX) {
                        isSourceEnabledFetchedOnce = true
                        processEvents()
                    }
                }
        }
    }

    override suspend fun intercept(event: Event): Event {
        LoggerAnalytics.debug("IntegrationsManagementPlugin: queueing event")

        runCatching {
            queuedEventsChannel.trySend(event).getOrThrow()
        }.onFailure {
            // drop the oldest event
            queuedEventsChannel.tryReceive()
            queuedEventsChannel.trySend(event)
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
            // todo: recheck this logic
            // if the source config is already fetched once and enabled, then initialise the destination since it is added after fetching of source config.
            if (isSourceEnabledFetchedOnce) {
                plugin.initDestination(sourceConfig)
            }
        }
    }

    internal fun removeIntegration(plugin: IntegrationPlugin) {
        integrationPluginChain.remove(plugin)
    }

    internal fun reset() {
        analytics.withIntegrationsDispatcher {
            integrationPluginChain.applyClosure { plugin ->
                if (plugin !is IntegrationPlugin) return@applyClosure

                if (plugin.isDestinationReady) {
                    plugin.reset()
                } else {
                    LoggerAnalytics.debug(
                        "IntegrationsManagementPlugin: Destination ${plugin.key} is not ready. Reset discarded."
                    )
                }
            }
        }
    }

    internal fun flush() {
        analytics.withIntegrationsDispatcher {
            integrationPluginChain.applyClosure { plugin ->
                if (plugin !is IntegrationPlugin) return@applyClosure

                if (plugin.isDestinationReady) {
                    plugin.flush()
                } else {
                    LoggerAnalytics.debug(
                        "IntegrationsManagementPlugin: Destination ${plugin.key} is not ready. Flush discarded."
                    )
                }
            }
        }
    }

    private fun processEvents() {
        analytics.withIntegrationsDispatcher {
            for (event in queuedEventsChannel) {
                integrationPluginChain.process(event)
            }
        }
    }
}

private fun Analytics.withIntegrationsDispatcher(block: suspend () -> Unit) {
    analyticsScope.launch(integrationsDispatcher) {
        block()
    }
}
