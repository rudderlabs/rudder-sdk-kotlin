package com.rudderstack.kotlin.sdk.plugins

import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.LoggerManager
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin

internal class PocPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    override fun execute(message: Message): Message {
        LoggerManager.debug("PocPlugin running")
        return message
    }
}
