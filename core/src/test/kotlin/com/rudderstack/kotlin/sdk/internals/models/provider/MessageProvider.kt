package com.rudderstack.kotlin.sdk.internals.models.provider

import com.rudderstack.kotlin.sdk.internals.models.ExternalIds
import com.rudderstack.kotlin.sdk.internals.models.emptyJsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun provideSampleJsonPayload() = buildJsonObject {
    put("key-1", "String value")
    put("key-2", 123)
    put("key-3", true)
    put("key-4", 123.456)
    put("key-5", buildJsonObject {
        put("key-6", "String value")
        put("key-7", 123)
        put("key-8", true)
        put("key-9", 123.456)
    })
    put("key-10", buildJsonArray {
        add("String value")
        add(123)
        add(true)
        add(123.456)
    })
    put("key-11", emptyJsonObject)
}

fun provideSampleIntegrationsPayload() = mapOf(
    "Amplitude" to true,
    "All" to false,
    "Firebase" to true,
    "Braze" to false,
)

fun provideSampleExternalIdsPayload() = listOf(
    ExternalIds("key-1", "value-1"),
    ExternalIds("key-2", "value-2"),
)
