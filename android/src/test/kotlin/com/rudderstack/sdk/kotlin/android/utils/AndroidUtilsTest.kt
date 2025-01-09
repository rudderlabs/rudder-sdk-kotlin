package com.rudderstack.sdk.kotlin.android.utils

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class AndroidUtilsTest {

    @Before
    fun setup() {
        mockkObject(AppSDKVersion)
    }

    @Test
    fun `when block is run on compatible SDK version, then run the compatible block`() {
        val minCompatibleVersion = 24
        val compatibleBlock = provideSpyBlock()
        val legacyBlock = provideSpyBlock()
        every { AppSDKVersion.getVersionSDKInt() } returns 24

        runBasedOnSDK(
            minCompatibleVersion = minCompatibleVersion,
            onCompatibleVersion = { compatibleBlock.execute() },
            onLegacyVersion = { legacyBlock.execute() },
        )

        verify { compatibleBlock.execute() }
    }

    @Test
    fun `when block is run on legacy SDK version, then run the legacy block`() {
        val minCompatibleVersion = 24
        val compatibleBlock = provideSpyBlock()
        val legacyBlock = provideSpyBlock()
        every { AppSDKVersion.getVersionSDKInt() } returns 21

        runBasedOnSDK(
            minCompatibleVersion = minCompatibleVersion,
            onCompatibleVersion = { compatibleBlock.execute() },
            onLegacyVersion = { legacyBlock.execute() },
        )

        verify { legacyBlock.execute() }
    }
}
