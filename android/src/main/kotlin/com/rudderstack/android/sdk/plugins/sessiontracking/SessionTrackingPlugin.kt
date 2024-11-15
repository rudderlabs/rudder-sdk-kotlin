package com.rudderstack.android.sdk.plugins.sessiontracking

import android.os.SystemClock
import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ProcessLifecycleObserver
import com.rudderstack.android.sdk.utils.addLifecycleObserver
import com.rudderstack.android.sdk.utils.mergeWithHigherPriorityTo
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.annotations.VisibleForTesting
import java.util.concurrent.TimeUnit
import com.rudderstack.android.sdk.Analytics as AndroidAnalytics
import com.rudderstack.android.sdk.Configuration as AndroidConfiguration

internal const val SESSION_ID = "sessionId"
internal const val SESSION_START = "sessionStart"

@OptIn(ExperimentalCoroutinesApi::class)
internal class SessionTrackingPlugin(
    // single thread dispatcher is required so that the session variables are updated (on storage) in a sequential manner.
    private val sessionDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess
    override lateinit var analytics: Analytics

    // this variable always holds the latest session id after the setup of this plugin is completed
    @Volatile
    private var sessionId = 0L

    @Volatile
    private var lastActivityTime = 0L

    @Volatile
    private var isSessionStart = false

    @Volatile
    private var isSessionManual = false

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        (analytics.configuration as? AndroidConfiguration)?.let { config ->
            // get the session variables from the storage
            sessionId = config.storage.readLong(StorageKeys.SESSION_ID, 0L)
            lastActivityTime = config.storage.readLong(StorageKeys.LAST_ACTIVITY_TIME, 0L)
            isSessionManual = config.storage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false)
            isSessionStart = config.storage.readBoolean(StorageKeys.IS_SESSION_START, false)

            when {
                config.sessionConfiguration.automaticSessionTracking -> {
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
                // end the session on launch if it is not manual
                !isSessionManual -> endSession()
                else -> Unit
            }
        }
    }

    override suspend fun execute(message: Message): Message {
        // add the session id to the message payload
        if (sessionId != 0L) {
            val sessionPayload = buildJsonObject {
                put(SESSION_ID, sessionId)
                if (isSessionStart) {
                    updateIsSessionStart(false)
                    put(SESSION_START, true)
                }
            }
            message.context = message.context mergeWithHigherPriorityTo sessionPayload
            if (!isSessionManual) {
                updateLastActivityTime()
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
            if (sessionId == 0L || isSessionManual || getMonotonicCurrentTime() - lastActivityTime >
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
            if (sessionId != 0L && !isSessionManual && getMonotonicCurrentTime() - lastActivityTime >
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
        updateIsSessionStart(true)
        updateIsSessionManual(isSessionManual)
        updateSessionId(sessionId)
    }

    private fun updateSessionId(sessionId: Long?) {
        if (this.sessionId != sessionId) {
            val updatedSessionId = sessionId ?: (TimeUnit.MILLISECONDS.toSeconds(getSystemCurrentTime()))
            this.sessionId = updatedSessionId
            analytics.analyticsScope.launch(sessionDispatcher) {
                analytics.configuration.storage.write(StorageKeys.SESSION_ID, updatedSessionId)
            }
        }
    }

    private fun updateIsSessionManual(isSessionManual: Boolean?) {
        if (isSessionManual != null && this.isSessionManual != isSessionManual) {
            this.isSessionManual = isSessionManual
            analytics.analyticsScope.launch(sessionDispatcher) {
                analytics.configuration.storage.write(StorageKeys.IS_SESSION_MANUAL, isSessionManual)
            }
        }
    }

    internal fun updateLastActivityTime() {
        val lastActivityTime = getMonotonicCurrentTime()
        this.lastActivityTime = lastActivityTime
        analytics.analyticsScope.launch(sessionDispatcher) {
            analytics.configuration.storage.write(StorageKeys.LAST_ACTIVITY_TIME, lastActivityTime)
        }
    }

    private fun updateIsSessionStart(isSessionStart: Boolean) {
        if (this.isSessionStart != isSessionStart) {
            this.isSessionStart = isSessionStart
            analytics.analyticsScope.launch(sessionDispatcher) {
                analytics.configuration.storage.write(StorageKeys.IS_SESSION_START, isSessionStart)
            }
        }
    }

    internal fun endSession() {
        sessionId = 0L
        lastActivityTime = 0L
        isSessionManual = false
        isSessionStart = false
        analytics.analyticsScope.launch(sessionDispatcher) {
            analytics.configuration.storage.remove(StorageKeys.SESSION_ID)
            analytics.configuration.storage.remove(StorageKeys.LAST_ACTIVITY_TIME)
            analytics.configuration.storage.remove(StorageKeys.IS_SESSION_MANUAL)
            analytics.configuration.storage.remove(StorageKeys.IS_SESSION_START)
        }
    }

    @VisibleForTesting
    internal fun getMonotonicCurrentTime() = SystemClock.elapsedRealtime()

    @VisibleForTesting
    internal fun getSystemCurrentTime() = System.currentTimeMillis()
}
