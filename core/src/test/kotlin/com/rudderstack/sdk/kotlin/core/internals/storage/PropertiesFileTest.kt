package com.rudderstack.sdk.kotlin.core.internals.storage

import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class PropertiesFileTest {

    private val directory = File("/tmp/rudderstack-analytics/123")
    private val propertiesFile = PropertiesFile(directory, "123")

    @BeforeEach
    fun setUp() {
        directory.deleteRecursively()
        propertiesFile.load()
    }

    @Test
    fun `given an int value stored, when get int is called, the same value is retrieved`() {
        propertiesFile.save("int", 1)

        assertEquals(propertiesFile.getInt("int", 0), 1)
    }

    @Test
    fun `given a long value stored, when get long is called, the same value is retrieved`() {
        propertiesFile.save("long", 1L)

        assertEquals(propertiesFile.getLong("long", 0), 1L)
    }

    @Test
    fun `given a string value stored, when get string is called, the same value is retrieved`() {
        propertiesFile.save("string", "test")

        assertEquals(propertiesFile.getString("string", ""), "test")
    }

    @Test
    fun `given a boolean value stored, when get boolean is called, the same value is retrieved`() {
        propertiesFile.save("boolean", true)

        assertEquals(propertiesFile.getBoolean("boolean", false), true)
    }

    @Test
    fun `given no value stored for an int variable, when get int is called, default value is retrieved`() {
        val defaultValue = 1

        assertEquals(propertiesFile.getInt("int", defaultValue), defaultValue)
    }

    @Test
    fun `given no value stored for a long variable, when get long is called, default value is retrieved`() {
        val defaultValue = 1L

        assertEquals(propertiesFile.getLong("long", defaultValue), defaultValue)
    }

    @Test
    fun `given no value stored for a string variable, when get string is called, default value is retrieved`() {
        val defaultValue = "test"

        assertEquals(propertiesFile.getString("string", defaultValue), defaultValue)
    }

    @Test
    fun `given no value stored for a boolean variable, when get boolean is called, default value is retrieved`() {
        val defaultValue = true

        assertEquals(propertiesFile.getBoolean("boolean", defaultValue), defaultValue)
    }

    @Test
    fun `given an int value when the value is cleared, the default value is returned`() {
        propertiesFile.save("int", 1)
        propertiesFile.clear("int")

        assertEquals(propertiesFile.getInt("int", 0), 0)
    }

    @Test
    fun `given multiple values stored, when clear is called for one key, only that key is removed`() {
        propertiesFile.save("key1", "value1")
        propertiesFile.save("key2", "value2")

        propertiesFile.clear("key1")

        assertEquals(String.empty(), propertiesFile.getString("key1", String.empty()))
        assertEquals("value2", propertiesFile.getString("key2", String.empty()))
    }

    @Test
    fun `given a value stored, when clear is called and file is reloaded, the value remains cleared`() {
        propertiesFile.save("key", "value")

        propertiesFile.clear("key")
        // Create new instance to force reload from file
        val reloadedFile = PropertiesFile(directory, "123")
        reloadedFile.load()

        assertEquals("default", reloadedFile.getString("key", "default"))
    }

    @Test
    fun `given no value stored, when clear is called, no exception is thrown`() {
        // Should not throw
        propertiesFile.clear("nonExistentKey")

        // Other operations should still work
        propertiesFile.save("otherKey", "value")
        assertEquals("value", propertiesFile.getString("otherKey", String.empty()))
    }

}
