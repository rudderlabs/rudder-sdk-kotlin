package com.rudderstack.sdk.kotlin.core.internals.storage

import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution

/**
 * Interface defining a basic key-value storage mechanism.
 *
 * This interface provides methods for storing, retrieving, and clearing values associated with specific keys.
 * It supports various data types including integers, booleans, strings, and longs.
 * Implementations of this interface should handle the persistence of these values in a storage medium such as
 * a database, shared preferences, or in-memory cache.
 */
@InternalRudderApi
interface KeyValueStorage {

    /**
     * Retrieves an integer value associated with the specified key.
     *
     * If the key is not found in the storage, the provided default value is returned.
     *
     * @param key The key used to identify the stored integer value.
     * @param defaultVal The default value to return if the key is not found.
     * @return The integer value associated with the key, or [defaultVal] if the key is not found.
     */
    fun getInt(key: String, defaultVal: Int): Int

    /**
     * Retrieves a boolean value associated with the specified key.
     *
     * If the key is not found in the storage, the provided default value is returned.
     *
     * @param key The key used to identify the stored boolean value.
     * @param defaultVal The default value to return if the key is not found.
     * @return The boolean value associated with the key, or [defaultVal] if the key is not found.
     */
    fun getBoolean(key: String, defaultVal: Boolean): Boolean

    /**
     * Retrieves a string value associated with the specified key.
     *
     * If the key is not found in the storage, the provided default value is returned.
     *
     * @param key The key used to identify the stored string value.
     * @param defaultVal The default value to return if the key is not found.
     * @return The string value associated with the key, or [defaultVal] if the key is not found.
     */
    fun getString(key: String, defaultVal: String): String

    /**
     * Retrieves a long value associated with the specified key.
     *
     * If the key is not found in the storage, the provided default value is returned.
     *
     * @param key The key used to identify the stored long value.
     * @param defaultVal The default value to return if the key is not found.
     * @return The long value associated with the key, or [defaultVal] if the key is not found.
     */
    fun getLong(key: String, defaultVal: Long): Long

    /**
     * Saves an integer value associated with the specified key.
     *
     * This method overwrites any existing value associated with the key.
     *
     * @param key The key used to identify the storage location.
     * @param value The integer value to be stored.
     */
    fun save(key: String, value: Int)

    /**
     * Saves a boolean value associated with the specified key.
     *
     * This method overwrites any existing value associated with the key.
     *
     * @param key The key used to identify the storage location.
     * @param value The boolean value to be stored.
     */
    fun save(key: String, value: Boolean)

    /**
     * Saves a string value associated with the specified key.
     *
     * This method overwrites any existing value associated with the key.
     *
     * @param key The key used to identify the storage location.
     * @param value The string value to be stored.
     */
    fun save(key: String, value: String)

    /**
     * Saves a long value associated with the specified key.
     *
     * This method overwrites any existing value associated with the key.
     *
     * @param key The key used to identify the storage location.
     * @param value The long value to be stored.
     */
    fun save(key: String, value: Long)

    /**
     * Clears the value associated with the specified key.
     *
     * This method removes any value that is currently stored under the given key.
     *
     * @param key The key used to identify the storage location to be cleared.
     */
    fun clear(key: String)

    /**
     * This method deletes the shared preferences file entirely to ensure a clean state.
     *
     * **Note**: It is recommended to use this API during shutdown to ensure the file is not removed abruptly, which could lead to unexpected errors.
     */
    @UseWithCaution
    fun delete()
}
