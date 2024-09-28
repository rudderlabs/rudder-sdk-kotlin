package com.rudderstack.kotlin.plugins

import com.rudderstack.kotlin.Analytics
import com.rudderstack.kotlin.internals.logger.TAG
import com.rudderstack.kotlin.internals.models.Message
import com.rudderstack.kotlin.internals.plugins.Plugin

internal class PocPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    override fun execute(message: Message): Message {
        analytics.configuration.logger.debug(TAG, "PocPlugin running")
        return message
    }
}
