package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.internals.utils.Base64.decodeFromBase64
import com.rudderstack.sdk.kotlin.core.internals.utils.Base64.encodeToBase64
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextBytes

class Base64Test {

    @Test
    fun `given a byte array, when encoded and decoded using Base64 class, then the results match with java encoder results`() {
        (0..100).forEach { i ->
            val input = nextBytes(i * 10)

            val javaEncoded = java.util.Base64.getEncoder().encodeToString(input)
            val kotlinEncoded = input.encodeToBase64()
            assertEquals(javaEncoded, kotlinEncoded)

            assertArrayEquals(input, kotlinEncoded.decodeFromBase64())
        }
    }
}
