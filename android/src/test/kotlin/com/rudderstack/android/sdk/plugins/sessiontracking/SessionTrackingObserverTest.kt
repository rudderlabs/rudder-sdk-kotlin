package com.rudderstack.android.sdk.plugins.sessiontracking

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class SessionTrackingObserverTest {

    private lateinit var observer: SessionTrackingObserver
    private lateinit var mockPlugin: SessionTrackingPlugin

    @Before
    fun setUp() {
        mockPlugin = mockk(relaxed = true)
        observer = SessionTrackingObserver(mockPlugin)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given session is not already updated, when onCreate is called, then checkAndStartSessionOnForeground is invoked`() {
        // given
        observer = spyk(observer, recordPrivateCalls = true)
        observer.isSessionAlreadyUpdated.set(false)

        // when
        observer.onCreate(mockk<LifecycleOwner>())

        // then
        verify { mockPlugin.checkAndStartSessionOnForeground() }
    }

    @Test
    fun `given session is not already updated, when onStart is called, then checkAndStartSessionOnForeground is invoked`() {
        // given
        observer.isSessionAlreadyUpdated.set(false)

        // when
        observer.onStart(mockk<LifecycleOwner>())

        // then
        verify { mockPlugin.checkAndStartSessionOnForeground() }
    }

    @Test
    fun `given session is not already updated, when onActivityCreated is called, then checkAndStartSessionOnForeground is invoked`() {
        // given
        observer.isSessionAlreadyUpdated.set(false)

        // when
        observer.onActivityCreated(mockk<Activity>(), mockk<Bundle>())

        // then
        verify { mockPlugin.checkAndStartSessionOnForeground() }
    }

    @Test
    fun `given session is not already updated, when onActivityStarted is called, then checkAndStartSessionOnForeground is invoked`() {
        // given
        observer.isSessionAlreadyUpdated.set(false)

        // when
        observer.onActivityStarted(mockk<Activity>())

        // then
        verify { mockPlugin.checkAndStartSessionOnForeground() }
    }

    @Test
    fun `when onStop is called, then sessionAlreadyUpdated is set to false`() {
        // given
        observer.isSessionAlreadyUpdated.set(true)

        // when
        observer.onStop(mockk<LifecycleOwner>())

        // then
        assert(!observer.isSessionAlreadyUpdated.get())
    }

    @Test
    fun `given session is already updated, when updateSession is called, then checkAndStartSessionOnForeground is not invoked`() {
        // given
        observer.isSessionAlreadyUpdated.set(true)

        // when
        observer.onCreate(mockk<LifecycleOwner>()) // Triggers updateSession()

        // then
        verify(exactly = 0) { mockPlugin.checkAndStartSessionOnForeground() }
    }
}
