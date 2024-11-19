package com.rudderstack.android.sdk.plugins.sessiontracking

import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ProcessLifecycleObserver
import com.rudderstack.android.sdk.utils.addLifecycleObserver
import com.rudderstack.android.sdk.utils.getMonotonicCurrentTime
import com.rudderstack.android.sdk.utils.mergeWithHigherPriorityTo
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.logger.LoggerAnalytics
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import com.rudderstack.kotlin.sdk.internals.statemanagement.FlowState
import com.rudderstack.kotlin.sdk.internals.storage.Storage
import com.rudderstack.kotlin.sdk.internals.utils.DateTimeUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates
import com.rudderstack.android.sdk.Analytics as AndroidAnalytics
import com.rudderstack.android.sdk.Configuration as AndroidConfiguration

internal const val SESSION_ID = "sessionId"
internal const val SESSION_START = "sessionStart"

@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
internal class SessionTrackingPlugin(
    // single thread dispatcher is required so that the session variables are updated (on storage) in a sequential manner.
    private val sessionDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess
    override lateinit var analytics: Analytics
    private val storage: Storage
        get() = analytics.configuration.storage

    private lateinit var sessionState: FlowState<SessionState>

    private val sessionId
        get() = sessionState.value.sessionId
    private val lastActivityTime
        get() = sessionState.value.lastActivityTime
    private val isSessionManual
        get() = sessionState.value.isSessionManual
    private val isSessionStart
        get() = sessionState.value.isSessionStart

    private var sessionTimeout by Delegates.notNull<Long>()

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        (analytics.configuration as? AndroidConfiguration)?.let { config ->
            sessionTimeout = if (config.sessionConfiguration.sessionTimeoutInMillis >= 0) {
                config.sessionConfiguration.sessionTimeoutInMillis
            } else {
                LoggerAnalytics.error("Session timeout cannot be negative. Setting it to 0.")
                0L
            }
            // get the session variables from the storage
            sessionState = FlowState(SessionState.initialState(analytics.configuration.storage))

            when {
                config.sessionConfiguration.automaticSessionTracking -> {
                    checkAndStartSessionOnLaunch()
                    // attach the session tracking observer to process lifecycle
                    attachSessionTrackingObservers()
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
                    updateIsSessionStartIfChanged(false)
                    put(SESSION_START, true)
                }
            }
            message.context = message.context mergeWithHigherPriorityTo sessionPayload
            if (!isSessionStart) {
                updateLastActivityTime()
            }
        }
        return message
    }

    private fun checkAndStartSessionOnLaunch() {
        if (shouldStartNewSessionOnLaunch()) {
            startSession(sessionId = generateSessionId(), isSessionManual = false)
        }
    }

    fun checkAndStartSessionOnForeground() {
        if (shouldStartNewSessionOnForeground()) {
            startSession(sessionId = generateSessionId(), isSessionManual = false)
        }
    }

    fun refreshSession() {
        if (sessionId != 0L) {
            startSession(sessionId = generateSessionId(), shouldUpdateIsSessionManual = false)
        }
    }

    fun startSession(sessionId: Long, isSessionManual: Boolean = false, shouldUpdateIsSessionManual: Boolean = true) {
        updateIsSessionStartIfChanged(true)
        if (shouldUpdateIsSessionManual) {
            updateIsSessionManualIfChanged(isSessionManual)
        }
        updateSessionId(sessionId)
    }

    private fun attachSessionTrackingObservers() {
        val sessionTrackingObserver = SessionTrackingObserver(this)
        (analytics as? AndroidAnalytics)?.addLifecycleObserver(
            sessionTrackingObserver as ProcessLifecycleObserver
        )
        (analytics as? AndroidAnalytics)?.addLifecycleObserver(
            sessionTrackingObserver as ActivityLifecycleObserver
        )
    }

    private fun updateSessionId(sessionId: Long) {
        sessionState.dispatch(SessionState.UpdateSessionIdAction(sessionId))
        withSessionDispatcher {
            sessionState.value.storeSessionId(sessionId, storage)
        }
    }

    private fun updateIsSessionManualIfChanged(isSessionManual: Boolean) {
        if (this.isSessionManual != isSessionManual) {
            sessionState.dispatch(SessionState.UpdateIsSessionManualAction(isSessionManual))
            withSessionDispatcher {
                sessionState.value.storeIsSessionManual(isSessionManual, storage)
            }
        }
    }

    fun updateLastActivityTime() {
        val lastActivityTime = getMonotonicCurrentTime()
        sessionState.dispatch(SessionState.UpdateLastActivityTimeAction(lastActivityTime))
        withSessionDispatcher {
            sessionState.value.storeLastActivityTime(lastActivityTime, storage)
        }
    }

    private fun updateIsSessionStartIfChanged(isSessionStart: Boolean) {
        if (this.isSessionStart != isSessionStart) {
            sessionState.dispatch(SessionState.UpdateIsSessionStartAction(isSessionStart))
            withSessionDispatcher {
                sessionState.value.storeIsSessionStart(isSessionStart, storage)
            }
        }
    }

    fun endSession() {
        sessionState.dispatch(SessionState.EndSessionAction)
        withSessionDispatcher {
            sessionState.value.removeSessionData(storage)
        }
    }

    fun generateSessionId(): Long {
        return TimeUnit.MILLISECONDS.toSeconds(DateTimeUtils.getSystemCurrentTime())
    }

    private fun shouldStartNewSessionOnForeground(): Boolean {
        return sessionId != 0L && !isSessionManual && hasSessionTimedOut()
    }

    private fun shouldStartNewSessionOnLaunch(): Boolean {
        return sessionId == 0L || isSessionManual || hasSessionTimedOut()
    }

    private fun hasSessionTimedOut(): Boolean {
        return getMonotonicCurrentTime() - lastActivityTime > sessionTimeout
    }

    private fun withSessionDispatcher(block: suspend () -> Unit) {
        analytics.analyticsScope.launch(sessionDispatcher) { block() }
    }
}
