package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys

/**
 * Reads the value from the storage for the given key and returns it.
 *
 * This function currently used to read the `Traits` and `ExternalIds` from the storage
 * and convert them to the respective types and return them.
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
