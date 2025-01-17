package com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin

class MockDestinationCustomPlugin: Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess
    override lateinit var analytics: Analytics

    override suspend fun intercept(event: Event): Event? {
        println("MockDestinationCustomPlugin: Intercepting event: $event")
        return super.intercept(event)
    }

    override fun teardown() {
        println("MockDestinationCustomPlugin: Teardown")
    }
}
