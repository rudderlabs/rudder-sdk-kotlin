package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys

/**
 * Retrieves the value from storage for the given key, converts it to the appropriate type, and returns it.
 *
 * @param key The key to read the value for.
 * @param defaultValue The default value to return if the key is not found.
 * @return The value for the key if found, else the default value.
 */
internal inline fun <reified T> Storage.readValuesOrDefault(key: StorageKeys, defaultValue: T): T =
    readString(key = key, defaultVal = String.empty()).let {
        if (it.isNotEmpty()) {
            LenientJson.decodeFromString(it)
        } else {
            defaultValue
        }
    }
