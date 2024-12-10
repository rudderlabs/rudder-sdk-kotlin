package com.rudderstack.sdk.kotlin.core.internals.models.provider

import com.rudderstack.sdk.kotlin.core.applyMockedValues
import com.rudderstack.sdk.kotlin.core.internals.models.ExternalId
import com.rudderstack.sdk.kotlin.core.internals.models.Message
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
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

fun provideSampleIntegrationsPayload() = buildJsonObject {
    put("Amplitude", true)
    put("All", false)
    put("Firebase", true)
    put("Braze", false)
    put("INTERCOM", buildJsonObject {
        put("lookup", "phone")
    })
}

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
