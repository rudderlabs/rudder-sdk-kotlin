package com.rudderstack.android.sdk.plugins.sessiontracking

import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ProcessLifecycleObserver
import com.rudderstack.android.sdk.utils.addLifecycleObserver
import com.rudderstack.android.sdk.utils.mergeWithHigherPriorityTo
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.atomic.AtomicBoolean
import com.rudderstack.android.sdk.Analytics as AndroidAnalytics
import com.rudderstack.android.sdk.Configuration as AndroidConfiguration

private const val SESSION_ID = "sessionId"
private const val SESSION_START = "sessionStart"
private const val MILLIS_IN_SECOND = 1000

internal class SessionTrackingPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess
    override lateinit var analytics: Analytics

    // this variable always holds the latest session id after the setup of this plugin is completed
    @Volatile
    private var sessionId = 0L

    @Volatile
    private var lastEventTime = 0L

    private val isSessionStart = AtomicBoolean(false)

    @Volatile
    private var isSessionManual = false

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        (analytics.configuration as? AndroidConfiguration)?.let { config ->
            // get the session variables from the storage
            sessionId = config.storage.readLong(StorageKeys.SESSION_ID, 0L)
            lastEventTime = config.storage.readLong(StorageKeys.LAST_EVENT_TIME, 0L)
            isSessionManual = config.storage.readBoolean(StorageKeys.IS_MANUAL_SESSION, false)

            if (config.sessionConfiguration.automaticSessionTracking) {
                checkAndStartSessionOnLaunch()
                // attach the session tracking observer to process lifecycle
                val sessionTrackingObserver = SessionTrackingObserver(this)
                (analytics as? AndroidAnalytics)?.addLifecycleObserver(
                    sessionTrackingObserver as ProcessLifecycleObserver
                )
                (analytics as? AndroidAnalytics)?.addLifecycleObserver(
                    sessionTrackingObserver as ActivityLifecycleObserver
                )
            }
        }
    }

    override suspend fun execute(message: Message): Message {
        // add the session id to the message payload
        if (sessionId != 0L) {
            val sessionPayload = buildJsonObject {
                put(SESSION_ID, sessionId)
                if (isSessionStart.get()) {
                    put(SESSION_START, isSessionStart.getAndSet(false))
                }
            }
            message.context = message.context mergeWithHigherPriorityTo sessionPayload
            if (!isSessionManual) {
                updateLastEventTime(System.currentTimeMillis())
            }
        }
        return message
    }

    private fun checkAndStartSessionOnLaunch() {
        (analytics.configuration as? AndroidConfiguration)?.let { config ->
            // start a new automatic session on launch if
            // 1. session id is not present OR
            // 2. session is manual OR
            // 3. session timeout has occurred
            if (sessionId == 0L || isSessionManual || System.currentTimeMillis() - lastEventTime >
                config.sessionConfiguration.sessionTimeoutInMillis
            ) {
                startSession(isSessionManual = false)
            }
        }
    }

    internal fun checkAndStartSessionOnForeground() {
        (analytics.configuration as? AndroidConfiguration)?.let { config ->
            // start a new automatic session on foreground if
            // 1. session is not ended previously
            // AND
            // 2. session is not manual
            // AND
            // 3. session timeout has occurred
            if (sessionId != 0L && !isSessionManual && System.currentTimeMillis() - lastEventTime >
                config.sessionConfiguration.sessionTimeoutInMillis
            ) {
                startSession(isSessionManual = false)
            }
        }
    }

    internal fun refreshSession() {
        if (sessionId != 0L) {
            startSession()
        }
    }

    internal fun startSession(sessionId: Long? = null, isSessionManual: Boolean? = null) {
        isSessionStart.set(true)
        updateIsSessionManual(isSessionManual)
        updateSessionId(sessionId)
    }

    private fun updateSessionId(sessionId: Long?) {
        if (this.sessionId != sessionId) {
            this.sessionId = sessionId ?: (System.currentTimeMillis() / MILLIS_IN_SECOND)
            analytics.analyticsScope.launch(analytics.storageDispatcher) {
                analytics.configuration.storage.write(StorageKeys.SESSION_ID, this@SessionTrackingPlugin.sessionId)
            }
        }
    }

    private fun updateIsSessionManual(isSessionManual: Boolean?) {
        if (isSessionManual != null && this.isSessionManual != isSessionManual) {
            this.isSessionManual = isSessionManual
            analytics.analyticsScope.launch(analytics.storageDispatcher) {
                analytics.configuration.storage.write(StorageKeys.IS_MANUAL_SESSION, isSessionManual)
            }
        }
    }

    private suspend fun updateLastEventTime(lastEventTime: Long) {
        this.lastEventTime = lastEventTime
        analytics.configuration.storage.write(StorageKeys.LAST_EVENT_TIME, lastEventTime)
    }

    internal fun endSession() {
        sessionId = 0L
        lastEventTime = 0L
        analytics.analyticsScope.launch(analytics.storageDispatcher) {
            analytics.configuration.storage.remove(StorageKeys.SESSION_ID)
            analytics.configuration.storage.remove(StorageKeys.LAST_EVENT_TIME)
        }
    }
}
