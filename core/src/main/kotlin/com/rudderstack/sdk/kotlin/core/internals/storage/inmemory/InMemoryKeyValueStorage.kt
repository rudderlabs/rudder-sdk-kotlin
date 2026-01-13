package com.rudderstack.sdk.kotlin.core.internals.storage.inmemory

import com.rudderstack.sdk.kotlin.core.internals.storage.KeyValueStorage
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution
import java.util.concurrent.ConcurrentHashMap

/**
 * An in-memory implementation of [com.rudderstack.sdk.kotlin.core.internals.storage.KeyValueStorage] that uses a [java.util.concurrent.ConcurrentHashMap] for thread-safe storage.
 *
 * This implementation stores all values in memory and does not persist data across sessions.
 * It is useful for testing purposes or when persistence is not required.
 */
@InternalRudderApi
internal class InMemoryKeyValueStorage : KeyValueStorage {

    private val storage = ConcurrentHashMap<String, Any>()

    override fun getInt(key: String, defaultVal: Int): Int {
        return (storage[key] as? Int) ?: defaultVal
    }

    override fun getBoolean(key: String, defaultVal: Boolean): Boolean {
        return (storage[key] as? Boolean) ?: defaultVal
    }

    override fun getString(key: String, defaultVal: String): String {
        return (storage[key] as? String) ?: defaultVal
    }

    override fun getLong(key: String, defaultVal: Long): Long {
        return (storage[key] as? Long) ?: defaultVal
    }

    override fun save(key: String, value: Int) {
        storage[key] = value
    }

    override fun save(key: String, value: Boolean) {
        storage[key] = value
    }

    override fun save(key: String, value: String) {
        storage[key] = value
    }

    override fun save(key: String, value: Long) {
        storage[key] = value
    }

    override fun clear(key: String) {
        storage.remove(key)
    }

    @UseWithCaution
    override fun delete() {
        storage.clear()
    }
}
