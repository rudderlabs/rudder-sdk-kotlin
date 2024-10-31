package com.rudderstack.kotlin.sdk.internals.utils

import com.rudderstack.kotlin.sdk.internals.storage.Storage
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys

internal inline fun <reified T> Storage.readValuesOrDefault(key: StorageKeys, defaultValue: T): T =
    readString(key = key, defaultVal = String.empty()).let {
        if (it.isNotEmpty()) {
            LenientJson.decodeFromString(it)
        } else {
            defaultValue
        }
    }
