package com.rudderstack.core.internals.plugins

import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.models.Message

interface Plugin {

    val pluginType: PluginType

    var analytics: Analytics

    fun setup(analytics: Analytics) {
        this.analytics = analytics
    }

    fun execute(event: Message): Message? {
        return event
    }

    fun teardown() {}

    enum class PluginType {
        PreProcess, // Executed before event processing begins.
        OnProcess, // Executed as the first level of event processing.
        Destination, // Executed as events begin to pass off to destinations
        After, // Executed after all event processing is completed.  This can be used to perform cleanup operations, etc.
        Manual //  Executed only when called manually, such as Session.
    }
}
