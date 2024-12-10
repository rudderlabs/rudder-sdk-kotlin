package com.rudderstack.kotlin.core.internals.utils

import com.rudderstack.kotlin.core.internals.models.DEFAULT_SENT_AT_TIMESTAMP
import io.mockk.every
import io.mockk.mockkObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Date

class JsonSentAtUpdaterTest {

    private val jsonSentAtUpdater = JsonSentAtUpdater()

    private val mockCurrentTime = Date(0).toInstant().toString()

    @Before
    fun setup() {
        mockkObject(DateTimeUtils)
        every { DateTimeUtils.now() } returns mockCurrentTime
    }

    @Test
    fun `given a json value with correct sentAt field, when updateSentAt called, then the sentAt field is updated`() {
        provideValidJsons().forEach { (jsonString, expectedJsonString) ->
            val updatedJsonString = jsonSentAtUpdater.updateSentAt(jsonString)

            assertEquals(expectedJsonString, updatedJsonString)
        }
    }

    @Test
    fun `given a json value with incorrect sentAt field, when updateSentAt called, then the sentAt field is not updated`() {
        provideInvalidJsons().forEach { jsonString ->
            val updatedJsonString = jsonSentAtUpdater.updateSentAt(jsonString)

            assertEquals(jsonString, updatedJsonString)
        }
    }

    private fun provideValidJsons(): List<Pair<String, String>> = listOf(
        """{"type":"track","event":"Test Event","sentAt":"$DEFAULT_SENT_AT_TIMESTAMP"}""" to """{"type":"track","event":"Test Event","sentAt":"$mockCurrentTime"}""",
        """{"type":"track","event":"$DEFAULT_SENT_AT_TIMESTAMP","sentAt":"$DEFAULT_SENT_AT_TIMESTAMP"}""" to """{"type":"track","event":"$DEFAULT_SENT_AT_TIMESTAMP","sentAt":"$mockCurrentTime"}"""
    )

    private fun provideInvalidJsons(): List<String> = listOf(
        """{"type":"track","event":"Test Event","sentAt":"some_random_value"}""",
        """{"type":"track","event":"Test Event","sentAt" : "$DEFAULT_SENT_AT_TIMESTAMP"}""",
        """{"type":"track","event":"$DEFAULT_SENT_AT_TIMESTAMP","sentAt" : "$DEFAULT_SENT_AT_TIMESTAMP"}"""
    )
}
