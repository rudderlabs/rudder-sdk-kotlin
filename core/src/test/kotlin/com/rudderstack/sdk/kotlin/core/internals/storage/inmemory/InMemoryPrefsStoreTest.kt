package com.rudderstack.sdk.kotlin.core.internals.storage.inmemory

import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InMemoryPrefsStoreTest {

    private lateinit var inMemoryPrefsStore: InMemoryPrefsStore

    @BeforeEach
    fun setUp() {
        inMemoryPrefsStore = InMemoryPrefsStore()
    }

    // Int operations

    @Test
    fun `given an int value stored, when getInt is called, then the same value is retrieved`() {
        inMemoryPrefsStore.save("int_key", 42)

        assertEquals(42, inMemoryPrefsStore.getInt("int_key", 0))
    }

    @Test
    fun `given no int value stored, when getInt is called, then default value is returned`() {
        val defaultValue = 100

        assertEquals(defaultValue, inMemoryPrefsStore.getInt("non_existent_key", defaultValue))
    }

    // Boolean operations

    @Test
    fun `given a true boolean value stored, when getBoolean is called, then true is retrieved`() {
        inMemoryPrefsStore.save("bool_key", true)

        assertEquals(true, inMemoryPrefsStore.getBoolean("bool_key", false))
    }

    @Test
    fun `given a false boolean value stored, when getBoolean is called, then false is retrieved`() {
        inMemoryPrefsStore.save("bool_key", false)

        assertEquals(false, inMemoryPrefsStore.getBoolean("bool_key", true))
    }

    @Test
    fun `given no boolean value stored, when getBoolean is called, then default value is returned`() {
        val defaultValue = true

        assertEquals(defaultValue, inMemoryPrefsStore.getBoolean("non_existent_key", defaultValue))
    }

    // String operations

    @Test
    fun `given a string value stored, when getString is called, then the same value is retrieved`() {
        inMemoryPrefsStore.save("string_key", "test_value")

        assertEquals("test_value", inMemoryPrefsStore.getString("string_key", ""))
    }

    @Test
    fun `given no string value stored, when getString is called, then default value is returned`() {
        val defaultValue = "default"

        assertEquals(defaultValue, inMemoryPrefsStore.getString("non_existent_key", defaultValue))
    }

    @Test
    fun `given an empty string stored, when getString is called, then empty string is retrieved`() {
        inMemoryPrefsStore.save("string_key", "")

        assertEquals("", inMemoryPrefsStore.getString("string_key", "default"))
    }

    // Long operations

    @Test
    fun `given a long value stored, when getLong is called, then the same value is retrieved`() {
        inMemoryPrefsStore.save("long_key", 9876543210L)

        assertEquals(9876543210L, inMemoryPrefsStore.getLong("long_key", 0L))
    }

    @Test
    fun `given no long value stored, when getLong is called, then default value is returned`() {
        val defaultValue = 1234567890L

        assertEquals(defaultValue, inMemoryPrefsStore.getLong("non_existent_key", defaultValue))
    }

    // Clear operations

    @Test
    fun `given a value stored, when clear is called with that key, then default value is returned`() {
        inMemoryPrefsStore.save("key_to_clear", "some_value")

        inMemoryPrefsStore.clear("key_to_clear")

        assertEquals("default", inMemoryPrefsStore.getString("key_to_clear", "default"))
    }

    @Test
    fun `given multiple values stored, when clear is called for one key, then other keys remain`() {
        inMemoryPrefsStore.save("key1", "value1")
        inMemoryPrefsStore.save("key2", "value2")
        inMemoryPrefsStore.save("key3", 123)

        inMemoryPrefsStore.clear("key2")

        assertEquals("value1", inMemoryPrefsStore.getString("key1", "default"))
        assertEquals("default", inMemoryPrefsStore.getString("key2", "default"))
        assertEquals(123, inMemoryPrefsStore.getInt("key3", 0))
    }

    // Delete operations

    @OptIn(UseWithCaution::class)
    @Test
    fun `given multiple values stored, when delete is called, then all values are removed`() {
        inMemoryPrefsStore.save("int_key", 42)
        inMemoryPrefsStore.save("bool_key", true)
        inMemoryPrefsStore.save("string_key", "test")
        inMemoryPrefsStore.save("long_key", 123L)

        inMemoryPrefsStore.delete()

        assertEquals(0, inMemoryPrefsStore.getInt("int_key", 0))
        assertEquals(false, inMemoryPrefsStore.getBoolean("bool_key", false))
        assertEquals("default", inMemoryPrefsStore.getString("string_key", "default"))
        assertEquals(0L, inMemoryPrefsStore.getLong("long_key", 0L))
    }

    // Type mismatch scenarios

    @Test
    fun `given an int value stored, when getString is called for same key, then default string is returned`() {
        inMemoryPrefsStore.save("key", 42)

        assertEquals("default", inMemoryPrefsStore.getString("key", "default"))
    }

    @Test
    fun `given a string value stored, when getInt is called for same key, then default int is returned`() {
        inMemoryPrefsStore.save("key", "not_an_int")

        assertEquals(0, inMemoryPrefsStore.getInt("key", 0))
    }

    @Test
    fun `given a boolean value stored, when getLong is called for same key, then default long is returned`() {
        inMemoryPrefsStore.save("key", true)

        assertEquals(100L, inMemoryPrefsStore.getLong("key", 100L))
    }

    @Test
    fun `given a long value stored, when getBoolean is called for same key, then default boolean is returned`() {
        inMemoryPrefsStore.save("key", 123L)

        assertEquals(true, inMemoryPrefsStore.getBoolean("key", true))
    }

    // Multiple keys

    @Test
    fun `given multiple keys with different types stored, when getting each key, then correct values are returned`() {
        inMemoryPrefsStore.save("int_key", 42)
        inMemoryPrefsStore.save("bool_key", true)
        inMemoryPrefsStore.save("string_key", "hello")
        inMemoryPrefsStore.save("long_key", 9999999999L)

        assertEquals(42, inMemoryPrefsStore.getInt("int_key", 0))
        assertEquals(true, inMemoryPrefsStore.getBoolean("bool_key", false))
        assertEquals("hello", inMemoryPrefsStore.getString("string_key", ""))
        assertEquals(9999999999L, inMemoryPrefsStore.getLong("long_key", 0L))
    }

    // Overwrite scenarios

    @Test
    fun `given a value stored, when saving a new value with the same key, then new value is retrieved`() {
        inMemoryPrefsStore.save("key", "original")
        inMemoryPrefsStore.save("key", "updated")

        assertEquals("updated", inMemoryPrefsStore.getString("key", "default"))
    }

    @Test
    fun `given an int value stored, when saving a different type with same key, then new type is stored`() {
        inMemoryPrefsStore.save("key", 42)
        inMemoryPrefsStore.save("key", "now_a_string")

        assertEquals("now_a_string", inMemoryPrefsStore.getString("key", "default"))
        assertEquals(0, inMemoryPrefsStore.getInt("key", 0))
    }
}
