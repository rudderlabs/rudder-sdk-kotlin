package com.rudderstack.sdk.kotlin.core.javacompat

import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetEntries
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ResetEntriesBuilderTest {

    private lateinit var resetEntriesBuilder: ResetEntriesBuilder

    @BeforeEach
    fun setUp() {
        resetEntriesBuilder = ResetEntriesBuilder()
    }

    @Test
    fun `when ResetEntries object is created with only default values, then it should have default values`() {
        val resetEntries = resetEntriesBuilder.build()

        resetEntries.also {
            assertTrue(it.anonymousId)
            assertTrue(it.userId)
            assertTrue(it.traits)
        }
    }

    @Test
    fun `when setAnonymousId is set to false, then anonymousId should be false`() {
        val resetEntries = resetEntriesBuilder.setAnonymousId(false).build()

        assertFalse(resetEntries.anonymousId)
    }

    @Test
    fun `when setUserId is set to false, then userId should be false`() {
        val resetEntries = resetEntriesBuilder.setUserId(false).build()

        assertFalse(resetEntries.userId)
    }

    @Test
    fun `when setTraits is set to false, then traits should be false`() {
        val resetEntries = resetEntriesBuilder.setTraits(false).build()

        assertFalse(resetEntries.traits)
    }

    @Test
    fun `when all custom configurations are set, then the ResetEntries object should reflect those values`() {
        val actualResetEntries = resetEntriesBuilder
            .setAnonymousId(false)
            .setUserId(false)
            .setTraits(false)
            .build()

        val expectedResetEntries = ResetEntries(
            anonymousId = false,
            userId = false,
            traits = false
        )

        with(actualResetEntries) {
            assertEquals(expectedResetEntries.anonymousId, anonymousId)
            assertEquals(expectedResetEntries.userId, userId)
            assertEquals(expectedResetEntries.traits, traits)
        }
    }
}
