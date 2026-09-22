package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.utils.consentState
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginChain
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

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

    private var isSourceEnabledFetchedAtLeastOnce = false

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        integrationPluginChain.analytics = analytics
        analytics.withIntegrationsDispatcher {
            // A trigger, not a data join: the consent value is discarded and the init gate re-reads
            // it live. `flow` emits the current value first, so the combine still fires on the first
            // source config when setConsent is never called. The source-config arm must keep
            // skipping its seed, which reports an enabled source carrying no destinations.
            combine(
                analytics.sourceConfigState.observeDispatched(),
                analytics.consentState.flow
            ) { sourceConfig, _ -> sourceConfig }
                // Filtered after the combine: filtering the source-config flow first would keep the
                // last enabled config cached, so a later consent change would replay it and
                // reinitialize destinations for a source that has since been disabled.
                .filter { it.source.isSourceEnabled }
                .collectIndexed { index, sourceConfig ->
                    integrationPluginChain.applyClosure { plugin ->
                        if (plugin is IntegrationPlugin) {
                            plugin.initDestination(sourceConfig)
                        }
                    }

                    if (index == FIRST_INDEX) {
                        isSourceEnabledFetchedAtLeastOnce = true
                        processEvents()
                    }
                }
        }
    }

    override suspend fun intercept(event: Event): Event {
        analytics.logger.debug("IntegrationsManagementPlugin: queueing event (messageId=${event.messageId})")

        runCatching {
            queuedEventsChannel.trySend(event).getOrThrow()
        }.onFailure {
            analytics.logger.warn(
                "IntegrationsManagementPlugin: Event queue full — dropping oldest event to make room " +
                    "(messageId=${event.messageId})"
            )
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
            if (isSourceEnabledFetchedAtLeastOnce) {
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
                    analytics.logger.debug(
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
                    analytics.logger.debug(
                        "IntegrationsManagementPlugin: Destination ${plugin.key} is not ready. Flush discarded."
                    )
                }
            }
        }
    }

    // The collector in setup() shares this single-threaded dispatcher, and `process` usually completes
    // without suspending, so a backlog would otherwise be drained in full before a re-evaluation queued
    // by a consent change ever runs - and every event in it would meet a destination that is still not
    // ready. Yielding hands that re-evaluation its turn first.
    private fun processEvents() {
        analytics.withIntegrationsDispatcher {
            for (event in queuedEventsChannel) {
                yield()
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
