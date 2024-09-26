package com.rudderstack.core.utils

import com.rudderstack.core.internals.utils.Base64Encoder
import junit.framework.TestCase.assertEquals
import org.junit.Test

class Base64UtilsTest {

    @Test
    fun testBase64Encoding() {
        assertEquals(Base64Encoder.encode("".toByteArray()), "")
        assertEquals(Base64Encoder.encode("f".toByteArray()), "Zg==")
        assertEquals(Base64Encoder.encode("fo".toByteArray()), "Zm8=")
        assertEquals(Base64Encoder.encode("foo".toByteArray()), "Zm9v")
        assertEquals(Base64Encoder.encode("foob".toByteArray()), "Zm9vYg==")
        assertEquals(Base64Encoder.encode("fooba".toByteArray()), "Zm9vYmE=")
        assertEquals(Base64Encoder.encode("foobar".toByteArray()), "Zm9vYmFy")
        assertEquals(Base64Encoder.encode("Hello, World!".toByteArray()), "SGVsbG8sIFdvcmxkIQ==")
        assertEquals(Base64Encoder.encode("ABCD".toByteArray()), "QUJDRA==")
    }
}
