package com.rudderstack.kotlin.internals.storage

import com.rudderstack.kotlin.sdk.internals.storage.PropertiesFile
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class PropertiesFileTest {

    private val directory = File("/tmp/rudderstack-analytics/123")
    private val keyValueStorage = PropertiesFile(directory, "123")

    @Before
    fun setUp() {
        directory.deleteRecursively()
        keyValueStorage.load()
    }

    @Test
    fun `given an int value stored, when get int is called, the same value is retrieved`() {
        keyValueStorage.save("int", 1)

        assertEquals(keyValueStorage.getInt("int", 0), 1)
    }

    @Test
    fun `given a long value stored, when get long is called, the same value is retrieved`() {
        keyValueStorage.save("long", 1L)

        assertEquals(keyValueStorage.getLong("long", 0), 1L)
    }

    @Test
    fun `given a string value stored, when get string is called, the same value is retrieved`() {
        keyValueStorage.save("string", "test")

        assertEquals(keyValueStorage.getString("string", ""), "test")
    }

    @Test
    fun `given a boolean value stored, when get boolean is called, the same value is retrieved`() {
        keyValueStorage.save("boolean", true)

        assertEquals(keyValueStorage.getBoolean("boolean", false), true)
    }

    @Test
    fun `given an int value when the value is removed,test the default value is  returned`() {
        keyValueStorage.save("int", 1)
        keyValueStorage.remove("int")

        assertEquals(keyValueStorage.getInt("int", 0), 0)
    }

}
