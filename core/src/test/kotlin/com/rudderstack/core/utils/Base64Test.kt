package com.rudderstack.core.utils

import com.rudderstack.core.internals.utils.Base64.decodeFromBase64
import com.rudderstack.core.internals.utils.Base64.encodeToBase64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random.Default.nextBytes

class Base64Test {

    @Test
    fun encodeDecode() {
        (0..100).forEach { i ->
            val input = nextBytes(i * 10)

            val javaEncoded = java.util.Base64.getEncoder().encodeToString(input)
            val kotlinEncoded = input.encodeToBase64()
            assertEquals(javaEncoded, kotlinEncoded)

            assertArrayEquals(input, kotlinEncoded.decodeFromBase64())
        }
    }
}
