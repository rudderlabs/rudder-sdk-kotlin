package com.rudderstack.sdk.kotlin.android.connectivity

import android.app.Application
import android.content.BroadcastReceiver
import android.net.ConnectivityManager
import android.os.Build
import com.rudderstack.sdk.kotlin.android.utils.AppSDKVersion
import com.rudderstack.sdk.kotlin.android.utils.Block
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.android.utils.provideSpyBlock
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.invoke
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class AndroidConnectivityObserverTest {

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockNetworkCallback: ConnectivityManager.NetworkCallback

    @MockK
    private lateinit var mockBroadcastReceiver: BroadcastReceiver

    @MockK
    private lateinit var mockConnectivityManager: ConnectivityManager

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    private val networkCallbackAvailabilitySlot = slot<AtomicBoolean>()
    private val networkCallbackSubscribersSlot = slot<() -> Unit>()

    private val broadcastReceiverAvailabilitySlot = slot<AtomicBoolean>()
    private val broadcastReceiverSubscribersSlot = slot<() -> Unit>()

    private val sdkVersions = listOf(
        Build.VERSION_CODES.N,          // Compatible SDK version
        Build.VERSION_CODES.LOLLIPOP,   // Legacy SDK version
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        every { mockApplication.getSystemService(ConnectivityManager::class.java) } returns mockConnectivityManager

        mockkStatic(::createBroadcastReceiver, ::createNetworkCallback)
        every {
            createNetworkCallback(
                networkAvailable = capture(networkCallbackAvailabilitySlot),
                notifySubscriber = capture(networkCallbackSubscribersSlot),
            )
        } returns mockNetworkCallback
        every {
            createBroadcastReceiver(
                networkAvailable = capture(broadcastReceiverAvailabilitySlot),
                notifySubscriber = capture(broadcastReceiverSubscribersSlot),
            )
        } returns mockBroadcastReceiver

        mockkObject(AppSDKVersion)
    }

    @After
    fun tearDown() {
        unmockkStatic(::createNetworkCallback, ::createBroadcastReceiver)
    }

    @Test
    fun `given connection is not available initially, when connection gets available, then pending subscribers are notified`() =
        runTest(testDispatcher) {
            sdkVersions.forEach { sdkVersion ->
                val subscriber1 = provideSpyBlock()
                val subscriber2 = provideSpyBlock()
                every { AppSDKVersion.getVersionSDKInt() } returns sdkVersion
                provideAndroidConnectivityObserver().also {
                    // This will be added in the pending subscribers list and will be notified when the network is available.
                    it.immediateNotifyOrObserveConnectivity { subscriber1.execute() }
                    it.immediateNotifyOrObserveConnectivity { subscriber2.execute() }
                }

                simulateNetworkAvailability()

                assertTrue(networkCallbackSubscribersSlot.isCaptured)
                verifySubscribersAreNotified(subscriber1, subscriber2)
            }
        }

    @Test
    fun `given connection is available initially, when subscribed, then they are notified immediately`() =
        runTest(testDispatcher) {
            sdkVersions.forEach { sdkVersion ->
                val subscriber1 = provideSpyBlock()
                val subscriber2 = provideSpyBlock()
                every { AppSDKVersion.getVersionSDKInt() } returns sdkVersion
                val androidConnectivityObserver = provideAndroidConnectivityObserver()
                simulateNetworkAvailability()

                // This will be notified immediately as the network is available.
                androidConnectivityObserver.immediateNotifyOrObserveConnectivity { subscriber1.execute() }
                androidConnectivityObserver.immediateNotifyOrObserveConnectivity { subscriber2.execute() }

                verifySubscribersAreNotified(subscriber1, subscriber2)
            }
        }

    @Test
    fun `given initially connection is not available, when connection gets available, then both pending and new subscribers are notified`() =
        runTest(testDispatcher) {
            sdkVersions.forEach { sdkVersion ->
                val subscriber1 = provideSpyBlock()
                val subscriber2 = provideSpyBlock()
                every { AppSDKVersion.getVersionSDKInt() } returns sdkVersion
                val androidConnectivityObserver = provideAndroidConnectivityObserver().also {
                    // This will be added in the pending subscribers list and will be notified when the network is available.
                    it.immediateNotifyOrObserveConnectivity { subscriber1.execute() }
                }

                simulateNetworkAvailability()
                // This will be notified immediately as the network is available.
                androidConnectivityObserver.immediateNotifyOrObserveConnectivity { subscriber2.execute() }

                assertTrue(networkCallbackSubscribersSlot.isCaptured)
                verifySubscribersAreNotified(subscriber1, subscriber2)
            }
        }

    @Test
    fun `given some exception is thrown and compatible SDK version is used and initially no subscriber is present, when subscribed, then they are notified immediately`() =
        runTest(testDispatcher) {
            val subscriber1 = provideSpyBlock()
            val subscriber2 = provideSpyBlock()
            every { AppSDKVersion.getVersionSDKInt() } returns Build.VERSION_CODES.N
            // As per the API of getSystemService, it can throw RuntimeException.
            every { mockApplication.getSystemService(ConnectivityManager::class.java) } throws RuntimeException()
            val androidConnectivityObserver = provideAndroidConnectivityObserver()

            // They will be notified immediately.
            androidConnectivityObserver.immediateNotifyOrObserveConnectivity { subscriber1.execute() }
            androidConnectivityObserver.immediateNotifyOrObserveConnectivity { subscriber2.execute() }

            verifySubscribersAreNotified(subscriber1, subscriber2)
        }

    private fun provideAndroidConnectivityObserver() = AndroidConnectivityObserver(
        application = mockApplication,
        analyticsScope = mockAnalytics.analyticsScope,
    )

    private fun simulateNetworkAvailability() {
        if (networkCallbackAvailabilitySlot.isCaptured) {
            networkCallbackAvailabilitySlot.captured.set(true)
            networkCallbackSubscribersSlot.invoke()
        }
        if (broadcastReceiverAvailabilitySlot.isCaptured) {
            broadcastReceiverAvailabilitySlot.captured.set(true)
            broadcastReceiverSubscribersSlot.invoke()
        }
        testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun verifySubscribersAreNotified(subscriber1: Block, subscriber2: Block) {
        coVerify(exactly = 1) { subscriber1.execute() }
        coVerify(exactly = 1) { subscriber2.execute() }
    }
}


