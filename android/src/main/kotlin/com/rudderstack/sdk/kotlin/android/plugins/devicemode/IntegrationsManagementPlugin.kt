package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginChain
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
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

    private val isSourceEnabled: Boolean
        get() = analytics.sourceConfigState.value.source.isSourceEnabled

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        integrationPluginChain.analytics = analytics
        analytics.analyticsScope.launch(analytics.analyticsDispatcher) {
            analytics.sourceConfigState
                .filter { it.source.isSourceEnabled }
                .collectIndexed { index, sourceConfig ->
                    integrationPluginChain.applyClosure { plugin ->
                        if (index == FIRST_INDEX) {
                            if (plugin is IntegrationPlugin && plugin.destinationState == DestinationState.Uninitialised) {
                                initAndNotifyCallbacks(sourceConfig, plugin)
                            }
                            processEvents()
                        } else {
                            if (plugin is IntegrationPlugin) {
                                plugin.findAndUpdateDestination(sourceConfig)
                            }
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
        destinationReadyCallbacks.clear()
    }

    internal fun onDestinationReady(plugin: IntegrationPlugin, onReady: (Any?, DestinationResult) -> Unit) {
        destinationReadyCallbacks
            .getOrPut(plugin.key) { mutableListOf() }
            .add(onReady)

        if (integrationPluginChain.hasIntegration(plugin)) {
            notifyDestinationCallbacks(plugin)
        }
    }

    internal fun addIntegration(plugin: IntegrationPlugin) {
        integrationPluginChain.add(plugin)
        if (isSourceEnabled) {
            initAndNotifyCallbacks(analytics.sourceConfigState.value, plugin)
        }
    }

    internal fun removeIntegration(plugin: IntegrationPlugin) {
        integrationPluginChain.remove(plugin)
    }

    internal fun reset() {
        integrationPluginChain.applyClosure { plugin ->
            if (plugin is IntegrationPlugin) {
                if (plugin.destinationState.isReady()) {
                    plugin.reset()
                } else {
                    LoggerAnalytics.debug(
                        "IntegrationsManagementPlugin: Destination ${plugin.key} is " +
                            "not ready yet. Reset discarded."
                    )
                }
            }
        }
    }

    internal fun flush() {
        integrationPluginChain.applyClosure { plugin ->
            if (plugin is IntegrationPlugin) {
                if (plugin.destinationState.isReady()) {
                    plugin.flush()
                } else {
                    LoggerAnalytics.debug(
                        "IntegrationsManagementPlugin: Destination ${plugin.key} is " +
                            "not ready yet. Flush discarded."
                    )
                }
            }
        }
    }

    private fun initAndNotifyCallbacks(sourceConfig: SourceConfig, plugin: IntegrationPlugin) {
        plugin.findAndInitDestination(sourceConfig)
        notifyDestinationCallbacks(plugin)
    }

    @Synchronized
    private fun notifyDestinationCallbacks(plugin: IntegrationPlugin) {
        val callBacks = destinationReadyCallbacks[plugin.key]?.toList()
        callBacks?.forEach { callback ->
            when (val destinationState = plugin.destinationState) {
                is DestinationState.Ready -> notifyAndRemoveCallback(plugin, callback, Result.Success(Unit))
                is DestinationState.Failed -> notifyAndRemoveCallback(
                    plugin,
                    callback,
                    Result.Failure(error = destinationState.exception)
                )
                is DestinationState.Uninitialised -> Unit
            }
        }
    }

    private fun notifyAndRemoveCallback(
        plugin: IntegrationPlugin,
        callback: (Any?, DestinationResult) -> Unit,
        result: DestinationResult
    ) {
        callback.invoke(plugin.getUnderlyingInstance(), result)
        destinationReadyCallbacks[plugin.key]?.remove(callback)
        if (destinationReadyCallbacks[plugin.key].isNullOrEmpty()) {
            destinationReadyCallbacks.remove(plugin.key)
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

private fun PluginChain.hasIntegration(plugin: IntegrationPlugin): Boolean {
    return findAll(Plugin.PluginType.Destination, IntegrationPlugin::class).contains(plugin)
}
