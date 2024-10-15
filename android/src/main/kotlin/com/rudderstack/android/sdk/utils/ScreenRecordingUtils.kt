package com.rudderstack.android.sdk.utils

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val AUTOMATIC_KEY = "automatic"

internal fun automaticProperty() = buildJsonObject { put(AUTOMATIC_KEY, true) }
