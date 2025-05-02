package com.rudderstack.sdk.kotlin.core.javacompat

import com.rudderstack.sdk.kotlin.core.provideJsonObject
import com.rudderstack.sdk.kotlin.core.provideMap
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.jupiter.api.Test

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert

@OptIn(ExperimentalSerializationApi::class)
class JsonInteropHelperTest {

    @Test
    fun `given map with primitive values, when converted, then returns correct JsonObject`() {
        val originalMap: Map<String, Any?> = mapOf(
            "booleanKey" to true,
            "doubleKey" to 3.14,
            "intKey" to 42,
            "stringKey" to "stringValue",
            "nullKey" to null
        )

        val actualJsonObject: JsonObject = JsonInteropHelper.fromMap(originalMap)

        val expectedJson = buildJsonObject {
            put("booleanKey", true)
            put("doubleKey", 3.14)
            put("intKey", 42)
            put("stringKey", "stringValue")
            put("nullKey", null)
        }
        verifyResult(expectedJson.toString(), actualJsonObject.toString())
    }

    @Test
    fun `given map, with nested map, when converted, then returns correct nested JsonObject`() {
        val originalMap: Map<String, Any?> = mapOf(
            "topLevel" to "value",
            "mapKey" to mapOf(
                "nestedKey1" to "nestedValue1",
                "nestedKey2" to 123,
                "nestedKey3" to null
            ),
            "emptyMap" to emptyMap<String, Any>()
        )

        val actualJsonObject = JsonInteropHelper.fromMap(originalMap)

        val expectedJson = buildJsonObject {
            put("topLevel", "value")
            put("mapKey", buildJsonObject {
                put("nestedKey1", "nestedValue1")
                put("nestedKey2", 123)
                put("nestedKey3", null)
            })
            put("emptyMap", buildJsonObject {})
        }
        verifyResult(expectedJson.toString(), actualJsonObject.toString())
    }

    @Test
    fun `given map with list, when converted, then returns correct JsonObject with JsonArray`() {
        val originalMap: Map<String, Any> = mapOf(
            "listKey" to listOf(1, "two", true, null),
            "emptyList" to emptyList<Any>()
        )

        val actualJsonObject = JsonInteropHelper.fromMap(originalMap)

        val expectedJson = buildJsonObject {
            put("listKey", buildJsonArray {
                add(1)
                add("two")
                add(true)
                add(null)
            })
            put("emptyList", buildJsonArray {})
        }
        verifyResult(expectedJson.toString(), actualJsonObject.toString())
    }

    @Test
    fun `given map with complex nested structure, when converted, then returns correct JsonObject`() {
        val deeplyNested: Map<String, Any?> = provideMap()

        val actualJsonObject = JsonInteropHelper.fromMap(deeplyNested)

        val expectedJson = provideJsonObject()
        verifyResult(expectedJson.toString(), actualJsonObject.toString())
    }

    @Test
    fun `given map with empty map, when converted, then returns correct JsonObject`() {
        val emptyMap: Map<String, Any> = emptyMap()

        val actualJsonObject = JsonInteropHelper.fromMap(emptyMap)

        val expectedJson = buildJsonObject {}
        verifyResult(expectedJson.toString(), actualJsonObject.toString())
    }

    @Test
    fun `given map with unsupported type, when converted, then throws IllegalArgumentException`() {
        val mapWithUnsupportedType = mapOf(
            "invalidType" to Exception("test")
        )

        assertThrows<IllegalArgumentException> {
            JsonInteropHelper.fromMap(mapWithUnsupportedType)
        }
    }

    private fun verifyResult(expected: String, actual: String) {
        JSONAssert.assertEquals(expected, actual, true)
    }
}
