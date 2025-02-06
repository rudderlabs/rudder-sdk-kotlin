package com.rudderstack.integration.kotlin.adjust

import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import junit.framework.TestCase.assertEquals
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Test

class UtilTest {

    @Test
    fun `given JsonObject with all types of values, when getString is called, then it should return the string value`() {
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
                else -> String.empty() // Handles missing keys
            }

            assertEquals(expectedValue, jsonObject.getString(key))
        }
    }

    @Test
    fun `given JsonObject with all types of values, when getInt is called, then it should return the int value`() {
        val jsonObject = provideJsonObjectWithAllTypesOfValues<Int>()

        jsonObject.entries.forEachIndexed { index, (key, _) ->
            val expectedValue = when (index) {
                0 -> DEFAULT_INT // String
                1 -> 20 // Int
                2 -> DEFAULT_INT // Boolean
                3 -> 20 // Double
                4 -> 20 // Double
                5 -> 25 // Double
                6 -> DEFAULT_INT // JsonArray
                7 -> DEFAULT_INT // Nested JsonObject
                8 -> DEFAULT_INT // Explicit null value
                9 -> DEFAULT_INT // Empty JSON Object
                10 -> DEFAULT_INT // Empty JSON Array
                11 -> DEFAULT_INT // Empty array inside object
                12 -> 42 // Number as String
                13 -> DEFAULT_INT // Boolean as String
                14 -> DEFAULT_INT // Deeply nested JSON
                15 -> 0 // NaN
                16 -> Int.MAX_VALUE // Infinity
                17 -> Int.MIN_VALUE // -Infinity
                18 -> Int.MAX_VALUE // Max value
                19 -> Int.MIN_VALUE // Min value
                else -> DEFAULT_INT // Handles missing keys
            }

            assertEquals(expectedValue, jsonObject.getInt(key))
        }
    }

    @Test
    fun `given JsonObject with all types of values, when getLong is called, then it should return the long value`() {
        val jsonObject = provideJsonObjectWithAllTypesOfValues<Long>()

        jsonObject.entries.forEachIndexed { index, (key, _) ->
            val expectedValue = when (index) {
                0 -> DEFAULT_LONG // String
                1 -> 20L // Int
                2 -> DEFAULT_LONG // Boolean
                3 -> 20L // Double
                4 -> 20L // Double
                5 -> 25L // Double
                6 -> DEFAULT_LONG // JsonArray
                7 -> DEFAULT_LONG // Nested JsonObject
                8 -> DEFAULT_LONG // Explicit null value
                9 -> DEFAULT_LONG // Empty JSON Object
                10 -> DEFAULT_LONG // Empty JSON Array
                11 -> DEFAULT_LONG // Empty array inside object
                12 -> 42L // Number as String
                13 -> DEFAULT_LONG // Boolean as String
                14 -> DEFAULT_LONG // Deeply nested JSON
                15 -> 0 // NaN
                16 -> Long.MAX_VALUE // Infinity
                17 -> Long.MIN_VALUE // -Infinity
                18 -> Long.MAX_VALUE // Max value
                19 -> Long.MIN_VALUE // Min value
                else -> DEFAULT_LONG // Handles missing keys
            }

            assertEquals(expectedValue, jsonObject.getLong(key))
        }
    }

    @Test
    fun `given JsonObject with all types of values, when getDouble is called, then it should return the double value`() {
        val jsonObject = provideJsonObjectWithAllTypesOfValues<Double>()

        jsonObject.entries.forEachIndexed { index, (key, _) ->
            val expectedValue = when (index) {
                0 -> DEFAULT_DOUBLE // String
                1 -> 20.0 // Int
                2 -> DEFAULT_DOUBLE // Boolean
                3 -> 20.0 // Double
                4 -> 20.045 // Double
                5 -> 25.945 // Double
                6 -> DEFAULT_DOUBLE // JsonArray
                7 -> DEFAULT_DOUBLE // Nested JsonObject
                8 -> DEFAULT_DOUBLE // Explicit null value
                9 -> DEFAULT_DOUBLE // Empty JSON Object
                10 -> DEFAULT_DOUBLE // Empty JSON Array
                11 -> DEFAULT_DOUBLE // Empty array inside object
                12 -> 42.0 // Number as String
                13 -> DEFAULT_DOUBLE // Boolean as String
                14 -> DEFAULT_DOUBLE // Deeply nested JSON
                15 -> Double.NaN // NaN
                16 -> Double.POSITIVE_INFINITY // Infinity
                17 -> Double.NEGATIVE_INFINITY // -Infinity
                18 -> Double.MAX_VALUE // Max value
                19 -> Double.MIN_VALUE // Min value
                else -> DEFAULT_DOUBLE // Handles missing keys
            }

            assertEquals(expectedValue, jsonObject.getDouble(key))
        }
    }
}

private inline fun <reified T> provideJsonObjectWithAllTypesOfValues() =
    buildJsonObject {
        put("key1", "value1") // String
        put("key2", 20)       // Int
        put("key3", true)     // Boolean
        put("key4", 20.0)     // Double
        put("key5", 20.045)   // Double
        put("key6", 25.945)   // Double
        put("key7", buildJsonArray { // JsonArray
            add(1)
            add(2)
            add(3)
        })
        put("key8", buildJsonObject { // Nested JsonObject
            put("nestedKey1", "nestedValue")
            put("nestedKey2", 42)
        })
        put("key9", JsonNull) // Explicit null value
        put("key10", buildJsonObject { }) // Empty JSON Object
        put("key11", buildJsonArray { }) // Empty JSON Array
        put("key12", buildJsonObject { put("emptyArray", buildJsonArray { }) }) // Empty array inside object
        put("key13", "42") // Number as String
        put("key14", "true") // Boolean as String
        put("key15", buildJsonObject { // Deeply nested JSON
            put("level1", buildJsonObject {
                put("level2", buildJsonObject {
                    put("level3", "deepValue")
                })
            })
        })
        put("key16", getNaN<T>()) // NaN (May not serialize correctly)
        put("key17", getPositiveInfinity<T>()) // Infinity
        put("key18", getNegativeInfinity<T>()) // -Infinity
        put("key19", getMaxValue<T>()) // Max value
        put("key20", getMinValue<T>()) // Min value
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
