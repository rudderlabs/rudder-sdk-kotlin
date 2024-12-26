package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginChain

/*
 * This plugin will host all the device mode destination plugins in its PluginChain instance.
 */
internal class DeviceModeDestinationPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Destination

    override lateinit var analytics: Analytics

    private val pluginChain = PluginChain().also { it.analytics = analytics }

    init {
        add(StartupQueuePlugin(pluginChain))
    }

    fun add(plugin: Plugin) {
        pluginChain.add(plugin)
    }
}
