package com.rudderstack.kotlin.sdk.internals.utils

import com.rudderstack.kotlin.sdk.internals.models.ExternalIds
import com.rudderstack.kotlin.sdk.internals.models.emptyJsonObject
import com.rudderstack.kotlin.sdk.internals.storage.Storage
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

internal fun Storage.readTraitsAndDecodeOrDefault(key: StorageKeys) =
    readString(key = key, defaultVal = String.empty()).let {
        if (it.isNotEmpty()) {
            LenientJson.decodeFromString(it)
        } else {
            emptyJsonObject
        }
    }

internal fun Storage.readExternalIdAndDecodeOrDefault(key: StorageKeys): List<ExternalIds> =
    readString(key = key, defaultVal = String.empty()).let {
        if (it.isNotEmpty()) {
            // Parse the JSON string as a JsonObject
            val jsonObject = Json.parseToJsonElement(it).jsonObject
            // Get the JsonArray corresponding to "externalId" and deserialize it to a List<ExternalIds>
            val externalIdsArray = jsonObject["externalId"]?.jsonArray ?: JsonArray(emptyList())
            LenientJson.decodeFromJsonElement(externalIdsArray)
        } else {
            emptyList()
        }
    }
