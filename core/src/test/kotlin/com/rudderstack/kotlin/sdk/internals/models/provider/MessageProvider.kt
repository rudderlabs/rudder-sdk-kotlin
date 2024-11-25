package com.rudderstack.kotlin.sdk.internals.models.provider

import com.rudderstack.kotlin.sdk.applyMockedValues
import com.rudderstack.kotlin.sdk.internals.models.ExternalId
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.models.TrackEvent
import com.rudderstack.kotlin.sdk.internals.models.emptyJsonObject
import com.rudderstack.kotlin.sdk.internals.platform.PlatformType
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
    ExternalId("key-1", "value-1"),
    ExternalId("key-2", "value-2"),
)

fun provideEvent(platformType: PlatformType = PlatformType.Mobile): Message = TrackEvent(
    event = "Sample Event",
    properties = emptyJsonObject,
).also {
    it.applyMockedValues()
    it.updateData(platformType)
}
