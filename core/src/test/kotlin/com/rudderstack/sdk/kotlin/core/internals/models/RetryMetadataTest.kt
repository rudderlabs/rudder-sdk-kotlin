package com.rudderstack.sdk.kotlin.core.internals.models

import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class RetryMetadataTest {

    @Test
    fun `given valid json, when fromJson is called, then returns RetryMetadata`() {
        val json = """{"batchId":0,"attempt":2,"lastAttemptTimestampInMillis":1705831208450,"reason":"server-503"}"""

        val result = RetryMetadata.fromJson(json)

        assertNotNull(result)
        assertEquals(0, result!!.batchId)
        assertEquals(2, result.attempt)
        assertEquals(1705831208450L, result.lastAttemptTimestampInMillis)
        assertEquals("server-503", result.reason)
    }

    @Test
    fun `given RetryMetadata, when toJson is called, then returns valid json string`() {
        val metadata = RetryMetadata(batchId = 0, attempt = 1, lastAttemptTimestampInMillis = 1705831205200L, reason = "server-500")

        val json = metadata.toJson()

        val expectedJson = """{"batchId":0,"attempt":1,"lastAttemptTimestampInMillis":1705831205200,"reason":"server-500"}"""
        JSONAssert.assertEquals(expectedJson, json, true)
    }

    @Test
    fun `given empty string, when fromJson is called, then returns null`() {
        val result = RetryMetadata.fromJson(String.empty())

        assertNull(result)
    }

    @Test
    fun `given malformed json, when fromJson is called, then returns null`() {
        val result = RetryMetadata.fromJson("{invalid json}")

        assertNull(result)
    }

    @Test
    fun `given json with missing fields, when fromJson is called, then returns null`() {
        val result = RetryMetadata.fromJson("""{"batchId":0}""")

        assertNull(result)
    }

    @Test
    fun `given json with extra fields, when fromJson is called, then ignores extra fields`() {
        val json = """{"batchId":0,"attempt":1,"lastAttemptTimestampInMillis":100,"reason":"server-500","extra":"field"}"""

        val result = RetryMetadata.fromJson(json)

        assertNotNull(result)
        assertEquals(0, result!!.batchId)
    }
}
