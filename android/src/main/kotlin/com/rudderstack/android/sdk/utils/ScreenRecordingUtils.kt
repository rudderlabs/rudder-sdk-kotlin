package com.rudderstack.android.sdk.utils

import com.rudderstack.android.sdk.plugins.screenrecording.AUTOMATIC_KEY
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun automaticProperty() = buildJsonObject { put(AUTOMATIC_KEY, true) }
