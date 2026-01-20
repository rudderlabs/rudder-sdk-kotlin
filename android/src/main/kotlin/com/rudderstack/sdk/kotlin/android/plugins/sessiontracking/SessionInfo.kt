package com.rudderstack.sdk.kotlin.android.plugins.sessiontracking

import com.rudderstack.sdk.kotlin.core.internals.statemanagement.StateAction
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.empty

internal const val DEFAULT_SESSION_ID = 0L
internal const val DEFAULT_LAST_ACTIVITY_TIME = 0L

internal data class SessionInfo(
    val sessionId: Long,
    val lastActivityTime: Long,
    val isSessionManual: Boolean,
    val isSessionStart: Boolean,
    val bootId: String,
) {

    companion object {

        fun initialState(storage: Storage): SessionInfo {
            return SessionInfo(
                sessionId = storage.readLong(StorageKeys.SESSION_ID, DEFAULT_SESSION_ID),
                lastActivityTime = storage.readLong(StorageKeys.LAST_ACTIVITY_TIME, DEFAULT_LAST_ACTIVITY_TIME),
                isSessionManual = storage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false),
                isSessionStart = storage.readBoolean(StorageKeys.IS_SESSION_START, false),
                bootId = storage.readString(StorageKeys.BOOT_ID, String.empty()),
            )
        }
    }

    sealed interface SessionInfoAction : StateAction<SessionInfo>

    class UpdateSessionIdAction(
        private val sessionId: Long
    ) : SessionInfoAction {

        override fun reduce(currentState: SessionInfo): SessionInfo {
            return currentState.copy(sessionId = sessionId)
        }
    }

    class UpdateLastActivityTimeAction(
        private val lastActivityTime: Long
    ) : SessionInfoAction {

        override fun reduce(currentState: SessionInfo): SessionInfo {
            return currentState.copy(lastActivityTime = lastActivityTime)
        }
    }

    class UpdateBootIdAction(
        private val bootId: String
    ) : SessionInfoAction {

        override fun reduce(currentState: SessionInfo): SessionInfo {
            return currentState.copy(bootId = bootId)
        }
    }

    class UpdateIsSessionManualAction(
        private val isSessionManual: Boolean
    ) : SessionInfoAction {

        override fun reduce(currentState: SessionInfo): SessionInfo {
            return currentState.copy(isSessionManual = isSessionManual)
        }
    }

    class UpdateIsSessionStartAction(
        private val isSessionStart: Boolean
    ) : SessionInfoAction {

        override fun reduce(currentState: SessionInfo): SessionInfo {
            return currentState.copy(isSessionStart = isSessionStart)
        }
    }

    data object EndSessionAction : SessionInfoAction {

        override fun reduce(currentState: SessionInfo): SessionInfo {
            return SessionInfo(
                sessionId = 0L,
                lastActivityTime = 0L,
                isSessionManual = false,
                isSessionStart = false,
                bootId = String.empty(),
            )
        }
    }

    suspend fun storeSessionId(sessionId: Long, storage: Storage) {
        storage.write(StorageKeys.SESSION_ID, sessionId)
    }

    suspend fun storeLastActivityTime(lastActivityTime: Long, storage: Storage) {
        storage.write(StorageKeys.LAST_ACTIVITY_TIME, lastActivityTime)
    }

    suspend fun storeBootId(bootId: String, storage: Storage) {
        storage.write(StorageKeys.BOOT_ID, bootId)
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
        storage.remove(StorageKeys.BOOT_ID)
    }
}
