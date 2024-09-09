package com.rudderstack.core.internals.plugins

import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.models.Message

interface Plugin {

    val pluginType: PluginType

    var analytics: Analytics

    fun setup(analytics: Analytics) {
        this.analytics = analytics
    }

    fun execute(message: Message): Message? {
        return message
    }

    fun teardown() {}

    enum class PluginType {
        PreProcess, // Executed before message processing begins.
        OnProcess, // Executed as the first level of message processing.
        Destination, // Executed as messages begin to pass off to destinations
        After, // Executed after all message processing is completed.  This can be used to perform cleanup operations, etc.
        Manual //  Executed only when called manually, such as Session.
    }
}
