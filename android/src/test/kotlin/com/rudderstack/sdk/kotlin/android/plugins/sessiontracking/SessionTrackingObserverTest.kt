package com.rudderstack.sdk.kotlin.android.plugins.sessiontracking

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SessionTrackingObserverTest {

    private lateinit var sessionTrackingObserver: SessionTrackingObserver

    @MockK
    private lateinit var mockSessionManager: SessionManager

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        sessionTrackingObserver = SessionTrackingObserver(mockSessionManager)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given session is not already updated, when onCreate is called, then checkAndStartSessionOnForeground is invoked`() {
        sessionTrackingObserver = spyk(sessionTrackingObserver, recordPrivateCalls = true)
        sessionTrackingObserver.isSessionAlreadyUpdated.set(false)

        sessionTrackingObserver.onCreate(mockk<LifecycleOwner>())

        verify { mockSessionManager.checkAndStartSessionOnForeground() }
    }

    @Test
    fun `given session is not already updated, when onStart is called, then checkAndStartSessionOnForeground is invoked`() {
        sessionTrackingObserver.isSessionAlreadyUpdated.set(false)

        sessionTrackingObserver.onStart(mockk<LifecycleOwner>())

        verify { mockSessionManager.checkAndStartSessionOnForeground() }
    }

    @Test
    fun `given session is not already updated, when onActivityCreated is called, then checkAndStartSessionOnForeground is invoked`() {
        sessionTrackingObserver.isSessionAlreadyUpdated.set(false)

        sessionTrackingObserver.onActivityCreated(mockk<Activity>(), mockk<Bundle>())

        verify { mockSessionManager.checkAndStartSessionOnForeground() }
    }

    @Test
    fun `given session is not already updated, when onActivityStarted is called, then checkAndStartSessionOnForeground is invoked`() {
        sessionTrackingObserver.isSessionAlreadyUpdated.set(false)

        sessionTrackingObserver.onActivityStarted(mockk<Activity>())

        verify { mockSessionManager.checkAndStartSessionOnForeground() }
    }

    @Test
    fun `given session is already updated, when onStop is called, then sessionAlreadyUpdated is set to false and updateLastActivityTime invoked`() {
        sessionTrackingObserver.isSessionAlreadyUpdated.set(true)

        sessionTrackingObserver.onStop(mockk<LifecycleOwner>())

        assert(!sessionTrackingObserver.isSessionAlreadyUpdated.get())
        verify { mockSessionManager.updateLastActivityTime() }
    }

    @Test
    fun `given session is already updated, when updateSession is called, then checkAndStartSessionOnForeground is not invoked`() {
        sessionTrackingObserver.isSessionAlreadyUpdated.set(true)

        sessionTrackingObserver.onCreate(mockk<LifecycleOwner>()) // Triggers updateSession()

        verify(exactly = 0) { mockSessionManager.checkAndStartSessionOnForeground() }
    }
}
