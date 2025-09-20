package com.rudderstack.sdk.kotlin.android.javacompat

import com.rudderstack.sdk.kotlin.android.models.reset.ResetEntries
import com.rudderstack.sdk.kotlin.android.models.reset.ResetOptions
import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetEntries as CoreResetEntries
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ResetOptionsBuilderTest {

    private lateinit var resetOptionsBuilder: ResetOptionsBuilder

    @BeforeEach
    fun setUp() {
        resetOptionsBuilder = ResetOptionsBuilder()
    }

    @Test
    fun `when ResetOptions object is created with only default values, then it should have default values`() {
        val resetOptions = resetOptionsBuilder.build()

        val expectedResetOptions = ResetOptions()
        assertEquals(expectedResetOptions, resetOptions)
    }

    @Test
    fun `when setEntries is set with core ResetEntries, then entries should be converted to Android ResetEntries`() {
        val coreEntries = CoreResetEntries(
            anonymousId = false,
            userId = false,
            traits = false
        )

        val resetOptions = resetOptionsBuilder.setEntries(coreEntries).build()

        val expectedEntries = ResetEntries(
            anonymousId = false,
            userId = false,
            traits = false,
            session = true // Should use default session value
        )

        assertEquals(expectedEntries, resetOptions.entries)
    }

    @Test
    fun `when setEntries is set with Android ResetEntries, then entries should be updated`() {
        val androidEntries = ResetEntries(
            anonymousId = false,
            userId = true,
            traits = false,
            session = false
        )

        val resetOptions = resetOptionsBuilder.setEntries(androidEntries).build()

        val expectedEntries = ResetEntries(
            anonymousId = false,
            userId = true,
            traits = false,
            session = false
        )

        assertEquals(expectedEntries, resetOptions.entries)
    }
}
