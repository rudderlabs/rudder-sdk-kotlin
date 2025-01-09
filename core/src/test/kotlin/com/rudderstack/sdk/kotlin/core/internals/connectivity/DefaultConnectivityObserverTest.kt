package com.rudderstack.sdk.kotlin.core.internals.connectivity

import com.rudderstack.sdk.kotlin.core.provideSpyBlock
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultConnectivityObserverTest {

    @Test
    fun `when subscriber is added, then they are notified immediately`() = runTest {
        val subscriber1 = provideSpyBlock()
        val subscriber2 = provideSpyBlock()
        val defaultObserver = DefaultConnectivityObserver()

        defaultObserver.notifyImmediatelyOrSubscribe {
            subscriber1.execute()
            defaultObserver.notifyImmediatelyOrSubscribe { subscriber2.execute() }

            coVerify { subscriber1.execute() }
            coVerify { subscriber2.execute() }
        }
    }
}
