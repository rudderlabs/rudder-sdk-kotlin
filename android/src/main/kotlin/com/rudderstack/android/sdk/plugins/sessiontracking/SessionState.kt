package com.rudderstack.android.sdk.plugins.sessiontracking

import com.rudderstack.kotlin.core.internals.statemanagement.FlowAction
import com.rudderstack.kotlin.core.internals.storage.Storage
import com.rudderstack.kotlin.core.internals.storage.StorageKeys

internal const val DEFAULT_SESSION_ID = 0L
internal const val DEFAULT_LAST_ACTIVITY_TIME = 0L

internal data class SessionState(
    val sessionId: Long,
    val lastActivityTime: Long,
    val isSessionManual: Boolean,
    val isSessionStart: Boolean,
) {

    companion object {

        fun initialState(storage: Storage): SessionState {
            return SessionState(
                sessionId = storage.readLong(StorageKeys.SESSION_ID, DEFAULT_SESSION_ID),
                lastActivityTime = storage.readLong(StorageKeys.LAST_ACTIVITY_TIME, DEFAULT_LAST_ACTIVITY_TIME),
                isSessionManual = storage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false),
                isSessionStart = storage.readBoolean(StorageKeys.IS_SESSION_START, false),
            )
        }
    }

    sealed interface SessionStateAction : FlowAction<SessionState>

    class UpdateSessionIdAction(
        private val sessionId: Long
    ) : SessionStateAction {

        override fun reduce(currentState: SessionState): SessionState {
            return currentState.copy(sessionId = sessionId)
        }
    }

    class UpdateLastActivityTimeAction(
        private val lastActivityTime: Long
    ) : SessionStateAction {

        override fun reduce(currentState: SessionState): SessionState {
            return currentState.copy(lastActivityTime = lastActivityTime)
        }
    }

    class UpdateIsSessionManualAction(
        private val isSessionManual: Boolean
    ) : SessionStateAction {

        override fun reduce(currentState: SessionState): SessionState {
            return currentState.copy(isSessionManual = isSessionManual)
        }
    }

    class UpdateIsSessionStartAction(
        private val isSessionStart: Boolean
    ) : SessionStateAction {

        override fun reduce(currentState: SessionState): SessionState {
            return currentState.copy(isSessionStart = isSessionStart)
        }
    }

    data object EndSessionAction : SessionStateAction {

        override fun reduce(currentState: SessionState): SessionState {
            return SessionState(
                sessionId = 0L,
                lastActivityTime = 0L,
                isSessionManual = false,
                isSessionStart = false,
            )
        }
    }

    suspend fun storeSessionId(sessionId: Long, storage: Storage) {
        storage.write(StorageKeys.SESSION_ID, sessionId)
    }

    suspend fun storeLastActivityTime(lastActivityTime: Long, storage: Storage) {
        storage.write(StorageKeys.LAST_ACTIVITY_TIME, lastActivityTime)
    }

    suspend fun storeIsSessionManual(isSessionManual: Boolean, storage: Storage) {
        storage.write(StorageKeys.IS_SESSION_MANUAL, isSessionManual)
    }

    suspend fun storeIsSessionStart(isSessionStart: Boolean, storage: Storage) {
        storage.write(StorageKeys.IS_SESSION_START, isSessionStart)
    }

    suspend fun removeSessionData(storage: Storage) {
        storage.remove(StorageKeys.SESSION_ID)
        storage.remove(StorageKeys.LAST_ACTIVITY_TIME)
        storage.remove(StorageKeys.IS_SESSION_MANUAL)
        storage.remove(StorageKeys.IS_SESSION_START)
    }
}
