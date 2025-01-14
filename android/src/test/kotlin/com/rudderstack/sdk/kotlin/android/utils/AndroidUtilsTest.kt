package com.rudderstack.sdk.kotlin.android.utils

import android.os.Build
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Before
import org.junit.Test

private const val MIN_SUPPORTED_VERSION = Build.VERSION_CODES.N // 24
class AndroidUtilsTest {

    @Before
    fun setup() {
        mockkObject(AppSDKVersion)
    }

    @Test
    fun `when block is targeted to run on compatible SDK version, then run that compatible block`() {
        val compatibleBlock = provideSpyBlock()
        val legacyBlock = provideSpyBlock()
        every { AppSDKVersion.getVersionSDKInt() } returns MIN_SUPPORTED_VERSION

        runBasedOnSDK(
            minCompatibleVersion = MIN_SUPPORTED_VERSION,
            onCompatibleVersion = { compatibleBlock.execute() },
            onLegacyVersion = { legacyBlock.execute() },
        )

        verify { compatibleBlock.execute() }
    }

    @Test
    fun `when block is targeted to run on legacy SDK version, then run that legacy block`() {
        val minCompatibleVersion = MIN_SUPPORTED_VERSION
        val compatibleBlock = provideSpyBlock()
        val legacyBlock = provideSpyBlock()
        every { AppSDKVersion.getVersionSDKInt() } returns Build.VERSION_CODES.LOLLIPOP // 21

        runBasedOnSDK(
            minCompatibleVersion = minCompatibleVersion,
            onCompatibleVersion = { compatibleBlock.execute() },
            onLegacyVersion = { legacyBlock.execute() },
        )

        verify { legacyBlock.execute() }
    }
}
