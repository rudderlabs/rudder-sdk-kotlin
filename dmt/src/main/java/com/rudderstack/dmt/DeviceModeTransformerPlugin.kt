package com.rudderstack.dmt

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin

class DeviceModeTransformerPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.OnProcess
    override lateinit var analytics: Analytics

    private lateinit var transformScriptManager: TransformScriptManager

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        transformScriptManager = TransformScriptManager(
            analytics = analytics,
            quickJSWrapper = QuickJSWrapper()
        )

        // call some method of TransformScriptManager to ensure the script is fetched and updated
        transformScriptManager.updateJSScriptAndNotify()
    }

    override suspend fun intercept(event: Event): Event? {
        return try {
            val transformedEvent = transformScriptManager.transformEvent(event)
            transformedEvent
        } catch (e: Exception) {
            LoggerAnalytics.error("JsonTransformationPlugin: Error transforming event - ${e.message}")
            event
        }
    }
}
