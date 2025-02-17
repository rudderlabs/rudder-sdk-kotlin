package com.rudderstack.integration.kotlin.braze

import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Parses the [JsonObject] to the specified type [T].
 */
@OptIn(InternalRudderApi::class)
internal inline fun <reified T> JsonObject.parse() = LenientJson.decodeFromJsonElement<T>(this)
