package com.rudderstack.android.sdk.plugins.sessiontracking

import com.rudderstack.android.sdk.utils.MockMemoryStorage
import com.rudderstack.kotlin.core.internals.storage.StorageKeys
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionStateTest {

    private val storage = spyk(MockMemoryStorage())

    @Test
    fun `given storage with session data, when initialState is called, then state is initialized correctly`() = runTest {
        storage.write(StorageKeys.SESSION_ID, 12345L)
        storage.write(StorageKeys.LAST_ACTIVITY_TIME, 67890L)
        storage.write(StorageKeys.IS_SESSION_MANUAL, true)
        storage.write(StorageKeys.IS_SESSION_START, false)

        val sessionState = SessionState.initialState(storage)

        assertEquals(12345L, sessionState.sessionId)
        assertEquals(67890L, sessionState.lastActivityTime)
        assertEquals(true, sessionState.isSessionManual)
        assertEquals(false, sessionState.isSessionStart)
    }

    @Test
    fun `given current state, when UpdateSessionIdAction is reduced, then sessionId is updated`() {
        val initialState = SessionState(1L, 0L, isSessionManual = false, isSessionStart = false)
        val action = SessionState.UpdateSessionIdAction(sessionId = 12345L)

        val newState = action.reduce(initialState)

        assertEquals(12345L, newState.sessionId)
        assertEquals(initialState.lastActivityTime, newState.lastActivityTime)
        assertEquals(initialState.isSessionManual, newState.isSessionManual)
        assertEquals(initialState.isSessionStart, newState.isSessionStart)
    }

    @Test
    fun `given current state, when UpdateLastActivityTimeAction is reduced, then lastActivityTime is updated`() {
        val initialState = SessionState(1L, 0L, isSessionManual = false, isSessionStart = false)
        val action = SessionState.UpdateLastActivityTimeAction(lastActivityTime = 67890L)

        val newState = action.reduce(initialState)

        assertEquals(67890L, newState.lastActivityTime)
        assertEquals(initialState.sessionId, newState.sessionId)
        assertEquals(initialState.isSessionManual, newState.isSessionManual)
        assertEquals(initialState.isSessionStart, newState.isSessionStart)
    }

    @Test
    fun `given current state, when UpdateIsSessionManualAction is reduced, then isSessionManual is updated`() {
        val initialState = SessionState(1L, 0L, isSessionManual = false, isSessionStart = false)
        val action = SessionState.UpdateIsSessionManualAction(isSessionManual = true)

        val newState = action.reduce(initialState)

        assertEquals(true, newState.isSessionManual)
        assertEquals(initialState.sessionId, newState.sessionId)
        assertEquals(initialState.lastActivityTime, newState.lastActivityTime)
        assertEquals(initialState.isSessionStart, newState.isSessionStart)
    }

    @Test
    fun `given current state, when UpdateIsSessionStartAction is reduced, then isSessionStart is updated`() {
        val initialState = SessionState(1L, 0L, isSessionManual = false, isSessionStart = false)
        val action = SessionState.UpdateIsSessionStartAction(isSessionStart = true)

        val newState = action.reduce(initialState)

        assertEquals(true, newState.isSessionStart)
        assertEquals(initialState.sessionId, newState.sessionId)
        assertEquals(initialState.lastActivityTime, newState.lastActivityTime)
        assertEquals(initialState.isSessionManual, newState.isSessionManual)
    }

    @Test
    fun `given current state, when EndSessionAction is reduced, then state is reset`() {
        val initialState = SessionState(12345L, 67890L, isSessionManual = true, isSessionStart = true)
        val action = SessionState.EndSessionAction

        val newState = action.reduce(initialState)

        assertEquals(0L, newState.sessionId)
        assertEquals(0L, newState.lastActivityTime)
        assertEquals(false, newState.isSessionManual)
        assertEquals(false, newState.isSessionStart)
    }

    @Test
    fun `given a sessionId, when storeSessionId is called, then sessionId is written to storage`() = runTest {
        val sessionState = SessionState(1L, 0L, isSessionManual = false, isSessionStart = false)

        sessionState.storeSessionId(12345L, storage)

        assertEquals(12345L, storage.readLong(StorageKeys.SESSION_ID, 0L))
    }

    @Test
    fun `given a lastActivityTime, when storeLastActivityTime is called, then lastActivityTime is written to storage`() = runTest {
        val sessionState = SessionState(1L, 0L, isSessionManual = false, isSessionStart = false)

        sessionState.storeLastActivityTime(67890L, storage)

        assertEquals(67890L, storage.readLong(StorageKeys.LAST_ACTIVITY_TIME, 0L))
    }

    @Test
    fun `given a isSessionManual, when storeIsSessionManual is called, then isSessionManual is written to storage`() = runTest {
        val sessionState = SessionState(1L, 0L, isSessionManual = false, isSessionStart = false)

        sessionState.storeIsSessionManual(true, storage)

        assertEquals(true, storage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false))
    }

    @Test
    fun `given isSessionStart value, when storeIsSessionStart is called, then isSessionStart is written to storage`() = runTest {
        val sessionState = SessionState(1L, 0L, isSessionManual = false, isSessionStart = false)

        sessionState.storeIsSessionStart(true, storage)

        assertEquals(true, storage.readBoolean(StorageKeys.IS_SESSION_START, false))
    }

    @Test
    fun `given session data in storage, when removeSessionData is called, then all session data is removed from storage`() = runTest {
        val sessionState = SessionState(1L, 0L, isSessionManual = false, isSessionStart = false)

        sessionState.removeSessionData(storage)

        coVerify {
            storage.remove(StorageKeys.SESSION_ID)
            storage.remove(StorageKeys.LAST_ACTIVITY_TIME)
            storage.remove(StorageKeys.IS_SESSION_MANUAL)
            storage.remove(StorageKeys.IS_SESSION_START)
        }
    }
}
