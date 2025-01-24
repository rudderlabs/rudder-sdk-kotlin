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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

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

    private val destinationReadyCallbacks = ConcurrentHashMap<String, MutableList<(Any?, DestinationResult) -> Unit>>()

    private val queuedEventsChannel: Channel<Event> = Channel(MAX_QUEUE_SIZE)

    @Volatile
    private var areDestinationsInitialised: Boolean = false
    private val mutex = Mutex()

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        integrationPluginChain.analytics = analytics
        analytics.analyticsScope.launch(analytics.analyticsDispatcher) {
            analytics.sourceConfigState
                .filter { it.source.isSourceEnabled }
                .collectIndexed { index, sourceConfig ->
                    val isFirstEmission = index == FIRST_INDEX

                    mutex.withLock {
                        integrationPluginChain.applySuspendingClosure { plugin ->
                            if (plugin is IntegrationPlugin) {
                                when (isFirstEmission) {
                                    true -> plugin.findAndInitDestination(sourceConfig)
                                    false -> plugin.findAndUpdateDestination(sourceConfig)
                                }
                            }
                        }
                        if (isFirstEmission) {
                            areDestinationsInitialised = true
                            processEvents()
                        }
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
        destinationReadyCallbacks.clear()
    }

    internal fun addIntegration(plugin: IntegrationPlugin) {
        integrationPluginChain.add(plugin)
        analytics.analyticsScope.launch {
            mutex.withLock {
                if (areDestinationsInitialised) {
                    plugin.findAndInitDestination(analytics.sourceConfigState.value)
                }
            }
        }
    }

    internal fun removeIntegration(plugin: IntegrationPlugin) {
        integrationPluginChain.remove(plugin)
    }

    internal fun reset() {
        integrationPluginChain.applyClosure { plugin ->
            if (plugin is IntegrationPlugin) {
                if (areDestinationsInitialised) {
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

    internal fun flush() {
        integrationPluginChain.applyClosure { plugin ->
            if (plugin is IntegrationPlugin) {
                if (areDestinationsInitialised) {
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

    private fun dropOldestAndSend(event: Event) {
        queuedEventsChannel.tryReceive()
        queuedEventsChannel.trySend(event)
    }

    private fun processEvents() {
        analytics.analyticsScope.launch(analytics.analyticsDispatcher) {
            for (event in queuedEventsChannel) {
                integrationPluginChain.process(event)
            }
        }
    }
}
