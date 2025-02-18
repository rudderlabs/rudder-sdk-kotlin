package com.rudderstack.integration.kotlin.adjust

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Test

class UtilTest {

    @Test
    fun `given JsonObject with all types of values, when attempts are made to cast them into string, then it should return the string value or null`() {
        val jsonObject = provideJsonObjectWithAllTypesOfValues<String>()

        jsonObject.entries.forEachIndexed { index, (key, _) ->
            val expectedValue = when (index) {
                0 -> "value1" // String
                1 -> "20" // Int
                2 -> "true" // Boolean
                3 -> "20.0" // Double
                4 -> "20.045" // Double
                5 -> "25.945" // Double
                6 -> "[1,2,3]" // JsonArray
                7 -> "{\"nestedKey1\":\"nestedValue\",\"nestedKey2\":42}" // Nested JsonObject
                8 -> "null" // Explicit null value
                9 -> "{}" // Empty JSON Object
                10 -> "[]" // Empty JSON Array
                11 -> "{\"emptyArray\":[]}" // Empty array inside object
                12 -> "42" // Number as String
                13 -> "true" // Boolean as String
                14 -> "{\"level1\":{\"level2\":{\"level3\":\"deepValue\"}}}" // Deeply nested JSON
                15 -> "NaN" // NaN
                16 -> "Infinity" // Infinity
                17 -> "-Infinity" // -Infinity
                18 -> Int.MAX_VALUE.toString() // Max value
                19 -> Int.MIN_VALUE.toString() // Min value
                20 -> "null" // Explicit null value
                else -> null
            }

            assertEquals(expectedValue, jsonObject.getStringOrNull(key))
        }
    }

    @Test
    fun `given JsonObject with all types of values, when attempts are made to cast them into int, then it should return the int value or null`() {
        val jsonObject = provideJsonObjectWithAllTypesOfValues<Int>()

        jsonObject.entries.forEachIndexed { index, (key, _) ->
            val expectedValue = when (index) {
                0 -> null // String
                1 -> 20 // Int
                2 -> null // Boolean
                3 -> 20 // Double
                4 -> 20 // Double
                5 -> 25 // Double
                6 -> null // JsonArray
                7 -> null // Nested JsonObject
                8 -> null // Explicit null value
                9 -> null // Empty JSON Object
                10 -> null // Empty JSON Array
                11 -> null // Empty array inside object
                12 -> 42 // Number as String
                13 -> null // Boolean as String
                14 -> null // Deeply nested JSON
                15 -> 0 // NaN
                16 -> Int.MAX_VALUE // Infinity
                17 -> Int.MIN_VALUE // -Infinity
                18 -> Int.MAX_VALUE // Max value
                19 -> Int.MIN_VALUE // Min value
                20 -> null // Explicit null value
                else -> null
            }

            assertEquals(expectedValue, jsonObject.getIntOrNull(key))
        }
    }

    @Test
    fun `given JsonObject with all types of values, when attempts are made to cast them into long, then it should return the long value or null`() {
        val jsonObject = provideJsonObjectWithAllTypesOfValues<Long>()

        jsonObject.entries.forEachIndexed { index, (key, _) ->
            val expectedValue = when (index) {
                0 -> null // String
                1 -> 20L // Int
                2 -> null // Boolean
                3 -> 20L // Double
                4 -> 20L // Double
                5 -> 25L // Double
                6 -> null // JsonArray
                7 -> null // Nested JsonObject
                8 -> null // Explicit null value
                9 -> null // Empty JSON Object
                10 -> null // Empty JSON Array
                11 -> null // Empty array inside object
                12 -> 42L // Number as String
                13 -> null // Boolean as String
                14 -> null // Deeply nested JSON
                15 -> 0 // NaN
                16 -> Long.MAX_VALUE // Infinity
                17 -> Long.MIN_VALUE // -Infinity
                18 -> Long.MAX_VALUE // Max value
                19 -> Long.MIN_VALUE // Min value
                20 -> null // Explicit null value
                else -> null
            }

            assertEquals(expectedValue, jsonObject.getLongOrNull(key))
        }
    }

    @Test
    fun `given JsonObject with all types of values, when attempts are made to cast them into double, then it should return the double value or null`() {
        val jsonObject = provideJsonObjectWithAllTypesOfValues<Double>()

        jsonObject.entries.forEachIndexed { index, (key, _) ->
            val expectedValue = when (index) {
                0 -> null // String
                1 -> 20.0 // Int
                2 -> null // Boolean
                3 -> 20.0 // Double
                4 -> 20.045 // Double
                5 -> 25.945 // Double
                6 -> null // JsonArray
                7 -> null // Nested JsonObject
                8 -> null // Explicit null value
                9 -> null // Empty JSON Object
                10 -> null // Empty JSON Array
                11 -> null // Empty array inside object
                12 -> 42.0 // Number as String
                13 -> null // Boolean as String
                14 -> null // Deeply nested JSON
                15 -> Double.NaN // NaN
                16 -> Double.POSITIVE_INFINITY // Infinity
                17 -> Double.NEGATIVE_INFINITY // -Infinity
                18 -> Double.MAX_VALUE // Max value
                19 -> Double.MIN_VALUE // Min value
                20 -> null // Explicit null value
                else -> null
            }

            assertEquals(expectedValue, jsonObject.getDoubleOrNull(key))
        }
    }

    @Test
    fun `given the key is not present in the JsonObject, when attempts are made to cast them into string, then it should return null`() {
        val jsonObject = provideJsonObjectWithAllTypesOfValues<String>()

        val value = jsonObject.getStringOrNull("key-not-present")

        assertNull(value)
    }

    @Test
    fun `given the key is not present in the JsonObject, when attempts are made to cast them into int, then it should return null`() {
        val jsonObject = provideJsonObjectWithAllTypesOfValues<Int>()

        val value = jsonObject.getIntOrNull("key-not-present")

        assertNull(value)
    }

    @Test
    fun `given the key is not present in the JsonObject, when attempts are made to cast them into long, then it should return null`() {
        val jsonObject = provideJsonObjectWithAllTypesOfValues<Long>()

        val value = jsonObject.getLongOrNull("key-not-present")

        assertNull(value)
    }

    @Test
    fun `given the key is not present in the JsonObject, when attempts are made to cast them into double, then it should return null`() {
        val jsonObject = provideJsonObjectWithAllTypesOfValues<Double>()

        val value = jsonObject.getDoubleOrNull("key-not-present")

        assertNull(value)
    }

    @Test
    fun `given event-to-token mapping contains the event, when retrieving the token, then return the corresponding token`() {
        val listOfEventToTokenMapping = provideListOfEventToTokenMapping()

        val token1 = listOfEventToTokenMapping.getTokenOrNull("Event-1")
        val token2 = listOfEventToTokenMapping.getTokenOrNull("Event-2")

        assertEquals("Token-1", token1)
        assertEquals("Token-2", token2)
    }

    @Test
    fun `given event-to-token mapping contains empty token, when retrieving the token, then return null`() {
        val listOfEventToTokenMapping = provideListOfEventToTokenMapping()

        val token = listOfEventToTokenMapping.getTokenOrNull("Event-3")

        assertNull(token)
    }

    @Test
    fun `given event-to-token mapping contains the event, when retrieving the token with case-sensitive event, then return null`() {
        val listOfEventToTokenMapping = provideListOfEventToTokenMapping()

        val token = listOfEventToTokenMapping.getTokenOrNull("event-4")

        assertNull(token)
    }

    @Test
    fun `given event-to-token mapping doesn't contains the event, when retrieving the token, then return null`() {
        val listOfEventToTokenMapping = provideListOfEventToTokenMapping()

        val token = listOfEventToTokenMapping.getTokenOrNull("Event-5")

        assertNull(token)
    }
}

@OptIn(ExperimentalSerializationApi::class)
private inline fun <reified T> provideJsonObjectWithAllTypesOfValues() =
    buildJsonObject {
        put("key0", "value1") // String
        put("key1", 20)       // Int
        put("key2", true)     // Boolean
        put("key3", 20.0)     // Double
        put("key4", 20.045)   // Double
        put("key5", 25.945)   // Double
        put("key6", buildJsonArray { // JsonArray
            add(1)
            add(2)
            add(3)
        })
        put("key7", buildJsonObject { // Nested JsonObject
            put("nestedKey1", "nestedValue")
            put("nestedKey2", 42)
        })
        put("key8", JsonNull) // Explicit null value
        put("key9", buildJsonObject { }) // Empty JSON Object
        put("key10", buildJsonArray { }) // Empty JSON Array
        put("key11", buildJsonObject { put("emptyArray", buildJsonArray { }) }) // Empty array inside object
        put("key12", "42") // Number as String
        put("key13", "true") // Boolean as String
        put("key14", buildJsonObject { // Deeply nested JSON
            put("level1", buildJsonObject {
                put("level2", buildJsonObject {
                    put("level3", "deepValue")
                })
            })
        })
        put("key15", getNaN<T>()) // NaN (May not serialize correctly)
        put("key16", getPositiveInfinity<T>()) // Infinity
        put("key17", getNegativeInfinity<T>()) // -Infinity
        put("key18", getMaxValue<T>()) // Max value
        put("key19", getMinValue<T>()) // Min value
        put("key20", null) // Explicit null value
    }

private inline fun <reified T> getNaN(): Number =
    when (T::class) {
        Float::class -> Float.NaN
        Double::class -> Double.NaN
        else -> Double.NaN
    }

private inline fun <reified T> getPositiveInfinity(): Number =
    when (T::class) {
        Float::class -> Float.POSITIVE_INFINITY
        Double::class -> Double.POSITIVE_INFINITY
        else -> Double.POSITIVE_INFINITY
    }

private inline fun <reified T> getNegativeInfinity(): Number =
    when (T::class) {
        Float::class -> Float.NEGATIVE_INFINITY
        Double::class -> Double.NEGATIVE_INFINITY
        else -> Double.NEGATIVE_INFINITY
    }

private inline fun <reified T> getMaxValue(): Number =
    when (T::class) {
        Int::class -> Int.MAX_VALUE
        Long::class -> Long.MAX_VALUE
        Float::class -> Float.MAX_VALUE
        Double::class -> Double.MAX_VALUE
        else -> Int.MAX_VALUE
    }

private inline fun <reified T> getMinValue(): Number =
    when (T::class) {
        Int::class -> Int.MIN_VALUE
        Long::class -> Long.MIN_VALUE
        Float::class -> Float.MIN_VALUE
        Double::class -> Double.MIN_VALUE
        else -> Int.MIN_VALUE
    }

private fun provideListOfEventToTokenMapping() = listOf(
    EventToTokenMapping(event = "Event-1", token = "Token-1"),
    EventToTokenMapping(event = "Event-2", token = "Token-2"),
    EventToTokenMapping(event = "Event-3", token = ""),
    EventToTokenMapping(event = "Event-4", token = "Token-4"),
)
