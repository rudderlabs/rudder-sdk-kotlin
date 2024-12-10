package com.rudderstack.kotlin.core.plugins

import com.rudderstack.kotlin.core.Analytics
import com.rudderstack.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.kotlin.core.internals.models.Message
import com.rudderstack.kotlin.core.internals.plugins.Plugin

internal class PocPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    override suspend fun execute(message: Message): Message {
        LoggerAnalytics.debug("PocPlugin running")
        return message
    }
}
