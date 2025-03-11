package com.rudderstack.sdk.kotlin.android.plugins.sessiontracking

import com.rudderstack.sdk.kotlin.android.utils.mergeWithHigherPriorityTo
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics

internal const val SESSION_ID = "sessionId"
internal const val SESSION_START = "sessionStart"

internal class SessionTrackingPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess
    override lateinit var analytics: Analytics

    private lateinit var sessionManager: SessionManager

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        (analytics as? AndroidAnalytics)?.let {
            sessionManager = analytics.sessionManager
        }
    }

    override fun teardown() {
        sessionManager.detachSessionTrackingObservers()
    }

    override suspend fun intercept(event: Event): Event {
        if (sessionManager.sessionId != DEFAULT_SESSION_ID) {
            addSessionIdToEvent(event)
            if (!sessionManager.isSessionManual) {
                sessionManager.updateLastActivityTime()
            }
        }
        return event
    }

    private fun addSessionIdToEvent(event: Event) {
        val sessionPayload = buildJsonObject {
            put(SESSION_ID, sessionManager.sessionId)
            if (sessionManager.isSessionStart) {
                sessionManager.updateIsSessionStartIfChanged(false)
                put(SESSION_START, true)
            }
        }
        event.context = event.context mergeWithHigherPriorityTo sessionPayload
    }
}
