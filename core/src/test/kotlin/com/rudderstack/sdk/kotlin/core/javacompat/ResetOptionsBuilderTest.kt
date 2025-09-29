package com.rudderstack.sdk.kotlin.core.javacompat

import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetEntries
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
        val defaultEntries = ResetEntries()

        val resetOptions = resetOptionsBuilder.build()

        with(resetOptions.entries) {
            assertEquals(defaultEntries.anonymousId, anonymousId)
            assertEquals(defaultEntries.userId, userId)
            assertEquals(defaultEntries.traits, traits)
        }
    }

    @Test
    fun `when setEntries is set with custom ResetEntries, then entries should be updated`() {
        val customEntries = ResetEntries(
            anonymousId = false,
            userId = false,
            traits = false
        )

        val resetOptions = resetOptionsBuilder.setEntries(customEntries).build()

        with(resetOptions.entries) {
            assertEquals(customEntries.anonymousId, anonymousId)
            assertEquals(customEntries.userId, userId)
            assertEquals(customEntries.traits, traits)
        }
    }
}
