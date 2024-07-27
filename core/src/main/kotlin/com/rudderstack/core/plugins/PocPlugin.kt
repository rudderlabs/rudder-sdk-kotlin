package com.rudderstack.core.plugins

import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.logger.TAG
import com.rudderstack.core.internals.models.Message
import com.rudderstack.core.internals.plugins.Plugin
import kotlinx.coroutines.launch

class PocPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        with(analytics) {
            analytics.analyticsScope.launch(analyticsDispatcher) {

            }
        }
    }

    override fun execute(event: Message): Message {
        analytics.configuration.logger.debug(TAG, "PocPlugin running")
        return event
    }

}
