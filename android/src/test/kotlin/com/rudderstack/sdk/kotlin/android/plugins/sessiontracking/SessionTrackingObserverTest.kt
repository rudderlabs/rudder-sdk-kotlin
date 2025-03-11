//package com.rudderstack.sdk.kotlin.android.plugins.sessiontracking
//
//import android.app.Activity
//import android.os.Bundle
//import androidx.lifecycle.LifecycleOwner
//import io.mockk.mockk
//import io.mockk.spyk
//import io.mockk.unmockkAll
//import io.mockk.verify
//import org.junit.jupiter.api.AfterEach
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//
//class SessionTrackingObserverTest {
//
//    private lateinit var observer: SessionTrackingObserver
//    private lateinit var mockPlugin: SessionTrackingPlugin
//
//    @BeforeEach
//    fun setUp() {
//        mockPlugin = mockk(relaxed = true)
//        observer = SessionTrackingObserver(mockPlugin)
//    }
//
//    @AfterEach
//    fun tearDown() {
//        unmockkAll()
//    }
//
//    @Test
//    fun `given session is not already updated, when onCreate is called, then checkAndStartSessionOnForeground is invoked`() {
//        observer = spyk(observer, recordPrivateCalls = true)
//        observer.isSessionAlreadyUpdated.set(false)
//
//        observer.onCreate(mockk<LifecycleOwner>())
//
//        verify { mockPlugin.checkAndStartSessionOnForeground() }
//    }
//
//    @Test
//    fun `given session is not already updated, when onStart is called, then checkAndStartSessionOnForeground is invoked`() {
//        observer.isSessionAlreadyUpdated.set(false)
//
//        observer.onStart(mockk<LifecycleOwner>())
//
//        verify { mockPlugin.checkAndStartSessionOnForeground() }
//    }
//
//    @Test
//    fun `given session is not already updated, when onActivityCreated is called, then checkAndStartSessionOnForeground is invoked`() {
//        observer.isSessionAlreadyUpdated.set(false)
//
//        observer.onActivityCreated(mockk<Activity>(), mockk<Bundle>())
//
//        verify { mockPlugin.checkAndStartSessionOnForeground() }
//    }
//
//    @Test
//    fun `given session is not already updated, when onActivityStarted is called, then checkAndStartSessionOnForeground is invoked`() {
//        observer.isSessionAlreadyUpdated.set(false)
//
//        observer.onActivityStarted(mockk<Activity>())
//
//        verify { mockPlugin.checkAndStartSessionOnForeground() }
//    }
//
//    @Test
//    fun `given session is already updated, when onStop is called, then sessionAlreadyUpdated is set to false and updateLastActivityTime invoked`() {
//        observer.isSessionAlreadyUpdated.set(true)
//
//        observer.onStop(mockk<LifecycleOwner>())
//
//        assert(!observer.isSessionAlreadyUpdated.get())
//        verify { mockPlugin.updateLastActivityTime() }
//    }
//
//    @Test
//    fun `given session is already updated, when updateSession is called, then checkAndStartSessionOnForeground is not invoked`() {
//        observer.isSessionAlreadyUpdated.set(true)
//
//        observer.onCreate(mockk<LifecycleOwner>()) // Triggers updateSession()
//
//        verify(exactly = 0) { mockPlugin.checkAndStartSessionOnForeground() }
//    }
//}
