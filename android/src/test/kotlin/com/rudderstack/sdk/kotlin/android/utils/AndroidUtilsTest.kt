package com.rudderstack.sdk.kotlin.android.utils

import android.os.Build
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val MIN_SUPPORTED_VERSION = Build.VERSION_CODES.N // 24
class AndroidUtilsTest {

    private lateinit var compatibleBlock: Block
    private lateinit var legacyBlock: Block

    @BeforeEach
    fun setup() {
        mockkObject(AppSDKVersion)

        compatibleBlock = provideSpyBlock()
        legacyBlock = provideSpyBlock()
    }

    @Test
    fun `given the SDK version is at least the minimum supported version, when block is executed, then run the compatible block`() {
        every { AppSDKVersion.getVersionSDKInt() } returns MIN_SUPPORTED_VERSION

        runBasedOnSDK(
            minCompatibleVersion = MIN_SUPPORTED_VERSION,
            onCompatibleVersion = { compatibleBlock.execute() },
            onLegacyVersion = { legacyBlock.execute() },
        )

        verify { compatibleBlock.execute() }
    }

    @Test
    fun `given the SDK version is below the minimum supported version, when block is executed, then run the legacy block`() {
        every { AppSDKVersion.getVersionSDKInt() } returns Build.VERSION_CODES.LOLLIPOP // 21

        runBasedOnSDK(
            minCompatibleVersion = MIN_SUPPORTED_VERSION,
            onCompatibleVersion = { compatibleBlock.execute() },
            onLegacyVersion = { legacyBlock.execute() },
        )

        verify { legacyBlock.execute() }
    }
}
