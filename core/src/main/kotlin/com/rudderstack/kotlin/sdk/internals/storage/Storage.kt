package com.rudderstack.kotlin.sdk.internals.storage

import com.rudderstack.kotlin.sdk.internals.utils.empty

/**
 * MAX_PAYLOAD_SIZE represents the maximum size in bytes for a single message payload.
 */
const val MAX_PAYLOAD_SIZE = 32 * 1024 // 32 KB

/**
 * MAX_BATCH_SIZE represents the maximum size in bytes for a batch of messages.
 */
const val MAX_BATCH_SIZE = 500 * 1024 // 500 KB

/**
 * Interface representing a generic storage system for reading and writing data.
 *
 * Implementations of this interface should provide mechanisms to store, retrieve,
 * and manage data associated with specific keys. The interface supports reading and
 * writing operations for various data types, including Boolean, Int, Long, and String.
 * It also provides methods for removing stored data, performing rollover operations,
 * and retrieving file lists.
 */
interface Storage {

    /**
     * Writes a Boolean value to the storage associated with the given key.
     *
     * @param key The [StorageKeys] used to identify the storage location.
     * @param value The Boolean value to write.
     */
    suspend fun write(key: StorageKeys, value: Boolean)

    /**
     * Writes an Int value to the storage associated with the given key.
     *
     * @param key The [StorageKeys] used to identify the storage location.
     * @param value The Int value to write.
     */
    suspend fun write(key: StorageKeys, value: Int)

    /**
     * Writes a Long value to the storage associated with the given key.
     *
     * @param key The [StorageKeys] used to identify the storage location.
     * @param value The Long value to write.
     */
    suspend fun write(key: StorageKeys, value: Long)

    /**
     * Writes a String value to the storage associated with the given key.
     *
     * @param key The [StorageKeys] used to identify the storage location.
     * @param value The String value to write.
     */
    suspend fun write(key: StorageKeys, value: String)

    /**
     * Removes the value associated with the given key from the storage.
     *
     * @param key The [StorageKeys] used to identify the storage location to remove.
     */
    suspend fun remove(key: StorageKeys)

    /**
     * Optionally performs a rollover operation. This method can be used to handle
     * any necessary actions when the storage needs to be rolled over, such as archiving
     * or compressing old data.
     */
    suspend fun rollover()

    /**
     * Closes the storage instance and performs any necessary cleanup operations.
     * This method gets called during shutdown.
     */
    fun close()

    /**
     * Reads an Int value from the storage associated with the given key.
     *
     * @param key The [StorageKeys] used to identify the storage location.
     * @param defaultVal The default Int value to return if the key does not exist.
     * @return The stored Int value or the provided default value.
     */
    fun readInt(key: StorageKeys, defaultVal: Int): Int

    /**
     * Reads a Boolean value from the storage associated with the given key.
     *
     * @param key The [StorageKeys] used to identify the storage location.
     * @param defaultVal The default Boolean value to return if the key does not exist.
     * @return The stored Boolean value or the provided default value.
     */
    fun readBoolean(key: StorageKeys, defaultVal: Boolean): Boolean

    /**
     * Reads a Long value from the storage associated with the given key.
     *
     * @param key The [StorageKeys] used to identify the storage location.
     * @param defaultVal The default Long value to return if the key does not exist.
     * @return The stored Long value or the provided default value.
     */
    fun readLong(key: StorageKeys, defaultVal: Long): Long

    /**
     * Reads a String value from the storage associated with the given key.
     *
     * @param key The [StorageKeys] used to identify the storage location.
     * @param defaultVal The default String value to return if the key does not exist.
     * @return The stored String value or the provided default value.
     */
    fun readString(key: StorageKeys, defaultVal: String): String

    /**
     * Removes a file from the storage based on its file path.
     *
     * @param filePath The path of the file to remove.
     */
    fun remove(filePath: String)

    /**
     * Reads a list of file paths stored in the storage.
     *
     * @return A list of file paths as [String].
     */
    fun readFileList(): List<String>

    /**
     * Retrieves the version information of the library.
     *
     * @return An instance of [LibraryVersion] containing version details.
     */
    fun getLibraryVersion(): LibraryVersion
}

/**
 * Enum representing the available storage keys.
 *
 * @property key
 */
enum class StorageKeys(val key: String) {

    /**
     * Key for storing or retrieving Rudder messages.
     */
    MESSAGE("message"),

    /**
     * Key for storing or retrieving the whole source config payload.
     */
    SOURCE_CONFIG_PAYLOAD("source_config_payload"),

    /**
     * Key for storing if source if enabled to send events
     */
    SOURCE_IS_ENABLED("source_is_enabled"),

    /**
     *Key for storing client app version.
     */
    APP_VERSION("rudder.app_version"),

    /**
     *Key for storing client app build.
     */
    APP_BUILD("rudder.app_build"),

    /**
     *Key for storing the anonymous id of the client.
     */
    ANONYMOUS_ID("anonymous_id"),

    /**
     *Key for storing if the anonymous id has been set by the client.
     */
    IS_ANONYMOUS_ID_BY_CLIENT("is_anonymous_id_by_client"),

    /**
     *Key for storing the id of the client's device.
     */
    DEVICE_ID("device_id"),

    /**
     *Key for storing the user id of the client.
     */
    USER_ID("user_id"),

    /**
     *Key for storing the traits of the client.
     */
    TRAITS("traits"),

    /**
     * Key for storing the external ids.
     */
    EXTERNAL_IDS("external_ids"),
}

/**
 * Represents the versioning information of a library.
 * Provides methods to retrieve platform-specific details, version name, version code,
 * and to convert this information into a map format.
 */
interface LibraryVersion {

    /**
     * Returns the package name of the library.
     *
     * @return The package name as a [String].
     */
    fun getPackageName(): String

    /**
     * Returns the version name of the library.
     *
     * @return The version name as a [String].
     */
    fun getVersionName(): String

    /**
     * Returns the build version of the library.
     * This method is optional and can return an empty string if the build version is not available.
     *
     * @return The build version as a [String].
     */
    fun getBuildVersion(): String = String.empty()
}

/**
 * Interface for providing access to different storage implementations.
 *
 * Implementations of this interface are responsible for creating and providing
 * instances of the [Storage] interface based on provided parameters.
 */
interface StorageProvider {

    /**
     * Retrieves a [Storage] instance based on the provided write key and application context.
     *
     * @param writeKey A key used to identify the specific storage instance.
     * @param application The application context to be used in storage operations.
     * @return An instance of [Storage] configured based on the provided parameters.
     */
    fun getStorage(writeKey: String, application: Any): Storage
}
