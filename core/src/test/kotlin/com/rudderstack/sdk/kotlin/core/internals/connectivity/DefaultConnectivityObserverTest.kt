package com.rudderstack.sdk.kotlin.core.internals.connectivity

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultConnectivityObserverTest {

    @Test
    fun `when subscriber is added, then they are notified immediately`() = runTest {
        val subscriber1 = createSpySubscriber()
        val subscriber2 = createSpySubscriber()
        val defaultObserver = DefaultConnectivityObserver()

        defaultObserver.notifyImmediatelyOrSubscribe { subscriber1.subscribe() }
        defaultObserver.notifyImmediatelyOrSubscribe { subscriber2.subscribe() }

        coVerify { subscriber1.subscribe() }
        coVerify { subscriber2.subscribe() }
    }

    private fun createSpySubscriber(): NetworkObserver {
        return spyk(NetworkObserver()).apply {
            coEvery { subscribe() } just Runs
        }
    }
}

// As Mockk doesn't support spying on suspend lambda function, we need to create a class for the same.
private class NetworkObserver {

    fun subscribe() {
        // Do nothing
    }
}
