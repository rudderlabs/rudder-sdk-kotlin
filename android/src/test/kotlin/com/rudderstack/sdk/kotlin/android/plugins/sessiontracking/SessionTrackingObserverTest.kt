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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun `given a background process start, when onCreate is called, then no session starts and the app is not marked foreground`() {
        sessionTrackingObserver = spyk(sessionTrackingObserver, recordPrivateCalls = true)
        sessionTrackingObserver.isSessionAlreadyUpdated.set(false)

        sessionTrackingObserver.onCreate(mockk<LifecycleOwner>())

        verify(exactly = 0) { mockSessionManager.checkAndStartSessionOnForeground() }
        assertFalse(sessionTrackingObserver.isInForeground.get())
    }

    @Test
    fun `given the observer is created, when the first onStart arrives, then checkAndStartSessionOnForeground is invoked`() {
        sessionTrackingObserver.onStart(mockk<LifecycleOwner>())

        verify(exactly = 1) { mockSessionManager.checkAndStartSessionOnForeground() }
    }

    @Test
    fun `given session is not already updated, when onResume is called, then checkAndStartSessionOnForeground is invoked`() {
        sessionTrackingObserver.isSessionAlreadyUpdated.set(false)

        sessionTrackingObserver.onResume(mockk<LifecycleOwner>())

        verify { mockSessionManager.checkAndStartSessionOnForeground() }
    }

    @Test
    fun `given app is in background, when onResume is called, then isInForeground is true`() {
        sessionTrackingObserver.onResume(mockk<LifecycleOwner>())

        assertTrue(sessionTrackingObserver.isInForeground.get())
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

        assertFalse(sessionTrackingObserver.isSessionAlreadyUpdated.get())
        verify { mockSessionManager.updateLastActivityTime() }
    }

    @Test
    fun `given session is already updated, when updateSession is called, then checkAndStartSessionOnForeground is not invoked`() {
        sessionTrackingObserver.isSessionAlreadyUpdated.set(true)

        sessionTrackingObserver.onStart(mockk<LifecycleOwner>()) // Triggers updateSession()

        verify(exactly = 0) { mockSessionManager.checkAndStartSessionOnForeground() }
    }

    @Test
    fun `given observer is created, when checked, then isInForeground is false`() {
        assertFalse(sessionTrackingObserver.isInForeground.get())
    }

    @Test
    fun `given app is in foreground, when onStop is called, then isInForeground is false`() {
        sessionTrackingObserver.onStart(mockk<LifecycleOwner>())

        sessionTrackingObserver.onStop(mockk<LifecycleOwner>())

        assertFalse(sessionTrackingObserver.isInForeground.get())
    }

    @Test
    fun `given app is in background, when onStart is called, then isInForeground is true`() {
        sessionTrackingObserver.onStart(mockk<LifecycleOwner>())

        assertTrue(sessionTrackingObserver.isInForeground.get())
    }
}
