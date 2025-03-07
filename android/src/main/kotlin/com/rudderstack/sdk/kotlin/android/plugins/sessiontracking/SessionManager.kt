package com.rudderstack.sdk.kotlin.android.plugins.sessiontracking

import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.DEFAULT_SESSION_TIMEOUT_IN_MILLIS
import com.rudderstack.sdk.kotlin.android.SessionConfiguration
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ProcessLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.addLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.getMonotonicCurrentTime
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.utils.DateTimeUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

@OptIn(ExperimentalCoroutinesApi::class)
internal class SessionManager(
    // single thread dispatcher is required so that the session variables are updated (on storage) in a sequential manner.
    private val sessionDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
    private val analytics: Analytics,
    sessionConfiguration: SessionConfiguration
) {

    private val storage: Storage
        get() = analytics.storage

    private var sessionState: State<SessionState> = State(SessionState.initialState(storage))
    internal var sessionTimeout by Delegates.notNull<Long>()

    internal val sessionId
        get() = sessionState.value.sessionId
    private val lastActivityTime
        get() = sessionState.value.lastActivityTime
    internal val isSessionManual
        get() = sessionState.value.isSessionManual
    internal val isSessionStart
        get() = sessionState.value.isSessionStart

    init {
        sessionTimeout = if (sessionConfiguration.sessionTimeoutInMillis >= 0) {
            sessionConfiguration.sessionTimeoutInMillis
        } else {
            LoggerAnalytics.error("Session timeout cannot be negative. Setting it to default value.")
            DEFAULT_SESSION_TIMEOUT_IN_MILLIS
        }

        when {
            sessionConfiguration.automaticSessionTracking -> {
                checkAndStartSessionOnLaunch()
                attachSessionTrackingObservers()
            }
            !isSessionManual -> endSession()
            else -> Unit
        }
    }

    /**
     * Starts a new session with the given session ID.
     *
     * @param sessionId The session ID to start the session with.
     * @param isSessionManual Flag to indicate if the session is manual or automatic. Defaults to `false`.
     * @param shouldUpdateIsSessionManual Flag to indicate if the `isSessionManual` should be updated. Defaults to `true`.
     */
    internal fun startSession(
        sessionId: Long,
        isSessionManual: Boolean = false,
        shouldUpdateIsSessionManual: Boolean = true
    ) {
        updateIsSessionStartIfChanged(true)
        if (shouldUpdateIsSessionManual) {
            updateIsSessionManualIfChanged(isSessionManual)
        }
        updateSessionId(sessionId)
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

    internal fun updateIsSessionStartIfChanged(isSessionStart: Boolean) {
        if (this.isSessionStart != isSessionStart) {
            sessionState.dispatch(SessionState.UpdateIsSessionStartAction(isSessionStart))
            withSessionDispatcher {
                sessionState.value.storeIsSessionStart(isSessionStart, storage)
            }
        }
    }

    private fun checkAndStartSessionOnLaunch() {
        if (shouldStartNewSessionOnLaunch()) {
            startSession(sessionId = generateSessionId(), isSessionManual = false)
        }
    }

    internal fun checkAndStartSessionOnForeground() {
        if (shouldStartNewSessionOnForeground()) {
            startSession(sessionId = generateSessionId(), isSessionManual = false)
        }
    }

    private fun attachSessionTrackingObservers() {
        val sessionTrackingObserver = SessionTrackingObserver(this)
        (analytics as? Analytics)?.addLifecycleObserver(
            sessionTrackingObserver as ProcessLifecycleObserver
        )
        (analytics as? Analytics)?.addLifecycleObserver(
            sessionTrackingObserver as ActivityLifecycleObserver
        )
    }

    internal fun refreshSession() {
        if (sessionId != DEFAULT_SESSION_ID) {
            startSession(sessionId = generateSessionId(), shouldUpdateIsSessionManual = false)
        }
    }

    internal fun endSession() {
        sessionState.dispatch(SessionState.EndSessionAction)
        withSessionDispatcher {
            sessionState.value.removeSessionData(storage)
        }
    }

    internal fun updateLastActivityTime() {
        val lastActivityTime = getMonotonicCurrentTime()
        sessionState.dispatch(SessionState.UpdateLastActivityTimeAction(lastActivityTime))
        withSessionDispatcher {
            sessionState.value.storeLastActivityTime(lastActivityTime, storage)
        }
    }

    private fun withSessionDispatcher(block: suspend () -> Unit) {
        analytics.analyticsScope.launch(sessionDispatcher) { block() }
    }

    private fun shouldStartNewSessionOnForeground(): Boolean {
        return sessionId != DEFAULT_SESSION_ID && !isSessionManual && hasSessionTimedOut()
    }

    private fun shouldStartNewSessionOnLaunch(): Boolean {
        return sessionId == DEFAULT_SESSION_ID || isSessionManual || hasSessionTimedOut()
    }

    private fun hasSessionTimedOut(): Boolean {
        return getMonotonicCurrentTime() - lastActivityTime > sessionTimeout
    }

    internal fun generateSessionId(): Long {
        return TimeUnit.MILLISECONDS.toSeconds(DateTimeUtils.getSystemCurrentTime())
    }
}
