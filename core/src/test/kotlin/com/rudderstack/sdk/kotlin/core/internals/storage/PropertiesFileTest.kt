package com.rudderstack.sdk.kotlin.core.internals.storage

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
    fun `given an int value when the value is removed,test the default value is  returned`() {
        propertiesFile.save("int", 1)
        propertiesFile.remove("int")

        assertEquals(propertiesFile.getInt("int", 0), 0)
    }

}
