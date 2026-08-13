package com.rudderstack.integration.kotlin.clevertap

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.BufferedReader

/**
 * Loads a JSON fixture from `src/test/resources/<fileName>` and parses it into a JsonObject.
 */
internal fun Any.readFileAsJsonObject(fileName: String): JsonObject {
    val inputStream = this::class.java.classLoader!!.getResourceAsStream(fileName)
    val jsonString = inputStream!!.bufferedReader().use(BufferedReader::readText)
    return Json.parseToJsonElement(jsonString).jsonObject
}
