package com.rudderstack.sdk.kotlin.core.internals.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Date

class DateTimeUtilsTest {

    @Test
    fun `now() should return current date in ISO 8601 format`() {
        val datePattern = Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z""")
        val formattedDate = DateTimeUtils.now()

        // Verify the format is correct
        assertTrue(datePattern.matches(formattedDate))
    }

    @Test
    fun `date string is converted correctly`() {
        val date = Date(1700617928023L)
        val dateTimeNowString = DateTimeUtils.from(date)
        assertEquals("2023-11-22T01:52:08.023Z", dateTimeNowString)
    }
}
