package com.rudderstack.integration.kotlin.sprig

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.userleap.Sprig
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UtilTest {

    private val mockLogger: Logger = mockk(relaxed = true)

    @BeforeEach
    fun setup() {
        mockkObject(Sprig)
        every { Sprig.setEmailAddress(any()) } just Runs
        every { Sprig.setVisitorAttribute(any(), any<String>()) } just Runs
        every { Sprig.setVisitorAttribute(any(), any<Int>()) } just Runs
        every { Sprig.setVisitorAttribute(any(), any<Boolean>()) } just Runs
    }

    // region parseConfig

    @Test
    fun `given valid config json, when parseConfig is called, then SprigConfig is returned`() {
        val configJson = buildJsonObject { put("environmentId", "envId123") }

        val result = configJson.parseConfig<SprigConfig>(mockLogger)

        assertEquals("envId123", result?.environmentId)
    }

    @Test
    fun `given empty config json, when parseConfig is called, then null is returned`() {
        val emptyConfig = JsonObject(emptyMap())

        val result = emptyConfig.parseConfig<SprigConfig>(mockLogger)

        assertNull(result)
    }

    // endregion

    // region setSprigAttributes

    @Test
    fun `given attributes with email, when setSprigAttributes is called, then setEmailAddress is called`() {
        val attributes = buildJsonObject { put("email", "user@test.com") }

        setSprigAttributes(Sprig, attributes, mockLogger)

        verify { Sprig.setEmailAddress("user@test.com") }
    }

    @Test
    fun `given attributes with string value, when setSprigAttributes is called, then setVisitorAttribute is called with string`() {
        val attributes = buildJsonObject { put("name", "Alice") }

        setSprigAttributes(Sprig, attributes, mockLogger)

        verify { Sprig.setVisitorAttribute("name", "Alice") }
    }

    @Test
    fun `given attributes with int value, when setSprigAttributes is called, then setVisitorAttribute is called with int`() {
        val attributes = buildJsonObject { put("age", 30) }

        setSprigAttributes(Sprig, attributes, mockLogger)

        verify { Sprig.setVisitorAttribute("age", 30) }
    }

    @Test
    fun `given attributes with boolean value, when setSprigAttributes is called, then setVisitorAttribute is called with boolean`() {
        val attributes = buildJsonObject { put("verified", true) }

        setSprigAttributes(Sprig, attributes, mockLogger)

        verify { Sprig.setVisitorAttribute("verified", true) }
    }

    @Test
    fun `given attributes with key starting with exclamation, when setSprigAttributes is called, then attribute is skipped`() {
        val attributes = buildJsonObject { put("!reserved", "value") }

        setSprigAttributes(Sprig, attributes, mockLogger)

        verify(exactly = 0) { Sprig.setVisitorAttribute(any(), any<String>()) }
        verify { mockLogger.warn(match { it.contains("not a valid property name") }) }
    }

    @Test
    fun `given attributes with key of 256 chars, when setSprigAttributes is called, then attribute is skipped`() {
        val longKey = "a".repeat(256)
        val attributes = buildJsonObject { put(longKey, "value") }

        setSprigAttributes(Sprig, attributes, mockLogger)

        verify(exactly = 0) { Sprig.setVisitorAttribute(any(), any<String>()) }
        verify { mockLogger.warn(match { it.contains("not a valid property name") }) }
    }

    @Test
    fun `given attributes with key of 255 chars, when setSprigAttributes is called, then attribute is accepted`() {
        val validKey = "a".repeat(255)
        val attributes = buildJsonObject { put(validKey, "value") }

        setSprigAttributes(Sprig, attributes, mockLogger)

        verify { Sprig.setVisitorAttribute(validKey, "value") }
    }

    @Test
    fun `given attributes with email and other traits, when setSprigAttributes is called, then email is set via setEmailAddress and others via setVisitorAttribute`() {
        val attributes = buildJsonObject {
            put("email", "user@test.com")
            put("name", "Alice")
            put("age", 30)
        }

        setSprigAttributes(Sprig, attributes, mockLogger)

        verify { Sprig.setEmailAddress("user@test.com") }
        verify { Sprig.setVisitorAttribute("name", "Alice") }
        verify { Sprig.setVisitorAttribute("age", 30) }
        // email should NOT be set as a visitor attribute
        verify(exactly = 0) { Sprig.setVisitorAttribute("email", any<String>()) }
    }

    // endregion

    // region toStringMap

    @Test
    fun `given json object with mixed types, when toStringMap is called, then values are correctly converted`() {
        val jsonObject = buildJsonObject {
            put("stringKey", "stringValue")
            put("intKey", 42)
            put("longKey", 9_999_999_999L)
            put("doubleKey", 3.75)
            put("boolKey", true)
        }

        val result = jsonObject.toStringMap()

        assertEquals("stringValue", result["stringKey"])
        assertEquals(42, result["intKey"])
        assertEquals(9_999_999_999L, result["longKey"])
        assertEquals(3.75, result["doubleKey"])
        assertEquals(true, result["boolKey"])
    }

    @Test
    fun `given empty json object, when toStringMap is called, then empty map is returned`() {
        val jsonObject = JsonObject(emptyMap())

        val result = jsonObject.toStringMap()

        assertEquals(emptyMap<String, Any>(), result)
    }

    @Test
    fun `given json object with JsonNull value, when toStringMap is called, then key is omitted`() {
        val jsonObject = buildJsonObject {
            put("name", "Alice")
            put("nullKey", JsonNull)
        }

        val result = jsonObject.toStringMap()

        assertEquals("Alice", result["name"])
        assertEquals(false, result.containsKey("nullKey"))
        assertEquals(1, result.size)
    }

    // endregion
}
