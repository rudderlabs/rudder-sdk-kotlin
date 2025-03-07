package com.rudderstack.sdk.kotlin.android

import android.app.Application
import android.content.BroadcastReceiver
import android.net.ConnectivityManager
import android.os.Build
import com.rudderstack.sdk.kotlin.android.connectivity.AndroidConnectivityObserverPlugin
import com.rudderstack.sdk.kotlin.android.connectivity.createBroadcastReceiver
import com.rudderstack.sdk.kotlin.android.connectivity.createNetworkCallback
import com.rudderstack.sdk.kotlin.android.utils.AppSDKVersion
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.connectivity.ConnectivityState
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import io.mockk.CapturingSlot
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val COMPATIBLE_SDK_VERSION = Build.VERSION_CODES.N // 24
private const val LEGACY_SDK_VERSION = Build.VERSION_CODES.LOLLIPOP // 21

class AndroidConnectivityObserverTest {

    @MockK
    private lateinit var mockAnalytics: Analytics

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockNetworkCallback: ConnectivityManager.NetworkCallback

    @MockK
    private lateinit var mockBroadcastReceiver: BroadcastReceiver

    @MockK
    private lateinit var mockConnectivityManager: ConnectivityManager

    @MockK
    private lateinit var mockConnectivityState: State<Boolean>

    private lateinit var connectivityStateSlot: CapturingSlot<State<Boolean>>

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        every { mockAnalytics.application } returns mockApplication
        every { mockApplication.getSystemService(ConnectivityManager::class.java) } returns mockConnectivityManager

        connectivityStateSlot = slot()
        mockkStatic(::createBroadcastReceiver, ::createNetworkCallback)
        every {
            createNetworkCallback(
                connectivityState = capture(connectivityStateSlot),
            )
        } returns mockNetworkCallback
        every {
            createBroadcastReceiver(
                connectivityState = capture(connectivityStateSlot),
            )
        } returns mockBroadcastReceiver

        mockkObject(AppSDKVersion)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::createNetworkCallback, ::createBroadcastReceiver)
    }

    @Test
    fun `given compatible sdk version, when connection gets available, then connection state is enabled`() {
        every { AppSDKVersion.getVersionSDKInt() } returns COMPATIBLE_SDK_VERSION
        provideAndroidConnectivityObserverPlugin()

        simulateNetworkAvailability()

        verifyConnectivityStateEnabled()
    }

    @Test
    fun `given legacy sdk version, when connection gets available, then connection state is enabled`() {
        every { AppSDKVersion.getVersionSDKInt() } returns LEGACY_SDK_VERSION
        provideAndroidConnectivityObserverPlugin()

        simulateNetworkAvailability()

        verifyConnectivityStateEnabled()
    }

    @Test
    fun `given compatible sdk version, when connection gets unavailable, then connection state is disabled`() {
        every { AppSDKVersion.getVersionSDKInt() } returns COMPATIBLE_SDK_VERSION
        provideAndroidConnectivityObserverPlugin()

        simulateNetworkUnAvailability()

        verifyConnectivityStateDisabled()
    }

    @Test
    fun `given legacy sdk version, when connection gets unavailable, then connection state is disabled`() {
        every { AppSDKVersion.getVersionSDKInt() } returns LEGACY_SDK_VERSION
        provideAndroidConnectivityObserverPlugin()

        simulateNetworkUnAvailability()

        verifyConnectivityStateDisabled()
    }

    // Note: Legacy SDK `register` API doesn't throws a runtime exception when registering for network callback.
    @Test
    fun `given compatible SDK version and runtime exception occurs while registering for network callback, when registering connectivity observer, then connection state should be enabled`() {
        every { AppSDKVersion.getVersionSDKInt() } returns COMPATIBLE_SDK_VERSION
        every { mockConnectivityManager.registerDefaultNetworkCallback(any()) } throws RuntimeException()

        provideAndroidConnectivityObserverPlugin()

        verifyDefaultConnectivityState()
    }

    @Test
    fun `given compatible SDK version, when teardown is called, then network callback is unregistered`() {
        every { AppSDKVersion.getVersionSDKInt() } returns COMPATIBLE_SDK_VERSION
        val plugin = provideAndroidConnectivityObserverPlugin()

        plugin.teardown()

        verify(exactly = 1) { mockConnectivityManager.unregisterNetworkCallback(mockNetworkCallback) }
    }

    @Test
    fun `given legacy SDK version, when teardown is called, then broadcast receiver is unregistered`() {
        every { AppSDKVersion.getVersionSDKInt() } returns LEGACY_SDK_VERSION
        val plugin = provideAndroidConnectivityObserverPlugin()

        plugin.teardown()

        verify(exactly = 1) { mockApplication.unregisterReceiver(mockBroadcastReceiver) }
    }

    private fun provideAndroidConnectivityObserverPlugin() = AndroidConnectivityObserverPlugin(
        connectivityState = mockConnectivityState,
    ).also { it.setup(mockAnalytics) }

    /**
     * Simulates both scenarios: network availability prior to and after SDK initialization.
     *
     * The [ConnectivityManager.NetworkCallback] `onAvailable` method is invoked whether the network is initially available or becomes available during runtime.
     */
    private fun simulateNetworkAvailability() {
        connectivityStateSlot.captured.dispatch(ConnectivityState.EnableConnectivityAction())
    }

    private fun simulateNetworkUnAvailability() {
        connectivityStateSlot.captured.dispatch(ConnectivityState.DisableConnectivityAction())
    }

    private fun verifyConnectivityStateEnabled() {
        assertTrue(connectivityStateSlot.isCaptured)
        verify(exactly = 1) {
            mockConnectivityState.dispatch(
                match { it is ConnectivityState.EnableConnectivityAction }
            )
        }
        verify(exactly = 0) {
            mockConnectivityState.dispatch(
                match { it is ConnectivityState.DisableConnectivityAction }
            )
        }
        verify(exactly = 0) {
            mockConnectivityState.dispatch(
                match { it is ConnectivityState.SetDefaultStateAction }
            )
        }
    }

    private fun verifyConnectivityStateDisabled() {
        assertTrue(connectivityStateSlot.isCaptured)
        verify(exactly = 1) {
            mockConnectivityState.dispatch(
                match { it is ConnectivityState.DisableConnectivityAction }
            )
        }
        verify(exactly = 0) {
            mockConnectivityState.dispatch(
                match { it is ConnectivityState.EnableConnectivityAction }
            )
        }
        verify(exactly = 0) {
            mockConnectivityState.dispatch(
                match { it is ConnectivityState.SetDefaultStateAction }
            )
        }
    }

    private fun verifyDefaultConnectivityState() {
        assertTrue(connectivityStateSlot.isCaptured)
        verify(exactly = 1) {
            mockConnectivityState.dispatch(
                match { it is ConnectivityState.SetDefaultStateAction }
            )
        }
        verify(exactly = 0) {
            mockConnectivityState.dispatch(
                match { it is ConnectivityState.EnableConnectivityAction }
            )
        }
        verify(exactly = 0) {
            mockConnectivityState.dispatch(
                match { it is ConnectivityState.DisableConnectivityAction }
            )
        }
    }
}
